package com.restaurant.qrorder.service;

import com.restaurant.qrorder.domain.entity.Discount;
import com.restaurant.qrorder.exception.custom.InvalidOperationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;


@Slf4j
@Service
public class DiscountCalculationService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public BigDecimal calculateDiscountAmount(
            Discount discount,
            BigDecimal orderTotal,
            Integer partySize,
            LocalDateTime orderDateTime
    ) {
        validateDiscountApplicability(discount, orderTotal, partySize, orderDateTime);

        BigDecimal discountAmount = BigDecimal.ZERO;

        switch (discount.getDiscountType()) {
            case ITEM_SPECIFIC -> discountAmount = calculatePercentageDiscount(discount, orderTotal);
            case HOLIDAY -> discountAmount = calculateHolidayDiscount(discount, orderTotal, orderDateTime);
            case PARTY_SIZE -> discountAmount = calculatePartySizeDiscount(discount, orderTotal, partySize);
            case BILL_TIER -> discountAmount = calculateBillTierDiscount(discount, orderTotal);
        }

        if (discount.getMaxDiscountAmount() != null &&
            discountAmount.compareTo(discount.getMaxDiscountAmount()) > 0) {
            discountAmount = discount.getMaxDiscountAmount();
        }

        return discountAmount;
    }

    public void validateDiscountApplicability(
            Discount discount,
            BigDecimal orderTotal,
            Integer partySize,
            LocalDateTime orderDateTime
    ) {
        if (!discount.getActive()) {
            throw new InvalidOperationException("Discount is not active");
        }

        if (discount.getStartDate() != null && orderDateTime.isBefore(discount.getStartDate())) {
            throw new InvalidOperationException("Discount has not started yet");
        }
        if (discount.getEndDate() != null && orderDateTime.isAfter(discount.getEndDate())) {
            throw new InvalidOperationException("Discount has expired");
        }

        if (discount.getUsageLimit() != null &&
                discount.getUsedCount() >= discount.getUsageLimit()) {
            throw new InvalidOperationException("Discount usage limit reached");
        }

        if (discount.getMinOrderAmount() != null &&
                orderTotal.compareTo(discount.getMinOrderAmount()) < 0) {
            throw new InvalidOperationException(
                    "Order total must be at least " + discount.getMinOrderAmount());
        }

        // Type-specific validation — checked here so isSilentlyApplicable filters bad data out
        switch (discount.getDiscountType()) {
            case BILL_TIER -> {
                if (discount.getTierConfig() == null || discount.getTierConfig().isBlank()) {
                    throw new InvalidOperationException(
                            "BILL_TIER discount '" + discount.getName() + "' has no tier configuration");
                }
            }
            case HOLIDAY -> validateHolidayDiscount(discount, orderDateTime);
            case PARTY_SIZE -> validatePartySizeDiscount(discount, partySize);
        }
    }

    private BigDecimal calculatePercentageDiscount(Discount discount, BigDecimal orderTotal) {
        switch (discount.getValueType()) {
            case PERCENTAGE -> {
                return orderTotal
                        .multiply(discount.getValue())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
            case FIXED_AMOUNT -> {
                return discount.getValue();
            }
            case FIXED_PRICE -> {
                return orderTotal.subtract(discount.getValue());
            }
            default -> throw new InvalidOperationException("Invalid discount value type");
        }
    }

    private BigDecimal calculateHolidayDiscount(
            Discount discount,
            BigDecimal orderTotal,
            LocalDateTime orderDateTime
    ) {
        validateHolidayDiscount(discount, orderDateTime);
        return calculatePercentageDiscount(discount, orderTotal);
    }

    private void validateHolidayDiscount(Discount discount, LocalDateTime orderDateTime) {
        String applicableDays = discount.getApplicableDays();
        if (applicableDays == null || applicableDays.isEmpty()) {
            throw new InvalidOperationException("Holiday discount requires applicable days configuration");
        }

        LocalDate orderDate = orderDateTime.toLocalDate();
        DayOfWeek orderDayOfWeek = orderDate.getDayOfWeek();

        List<String> days = Arrays.asList(applicableDays.split(","));

        boolean isApplicable = days.stream().anyMatch(day -> {
            day = day.trim();
            
            if (day.matches("\\d{4}-\\d{2}-\\d{2}")) {
                LocalDate specificDate = LocalDate.parse(day);
                return orderDate.isEqual(specificDate);
            }
            
            try {
                DayOfWeek dayOfWeek = DayOfWeek.valueOf(day.toUpperCase());
                return orderDayOfWeek == dayOfWeek;
            } catch (IllegalArgumentException e) {
                return false;
            }
        });

        if (!isApplicable) {
            throw new InvalidOperationException("Discount is not applicable on this day");
        }
    }

    private BigDecimal calculatePartySizeDiscount(
            Discount discount,
            BigDecimal orderTotal,
            Integer partySize
    ) {
        validatePartySizeDiscount(discount, partySize);

        return calculatePercentageDiscount(discount, orderTotal);
    }

    private void validatePartySizeDiscount(Discount discount, Integer partySize) {
        if (partySize == null) {
            throw new InvalidOperationException("Party size is required for this discount");
        }

        if (discount.getMinPartySize() != null && partySize < discount.getMinPartySize()) {
            throw new InvalidOperationException(
                "Minimum party size is " + discount.getMinPartySize()
            );
        }

        if (discount.getMaxPartySize() != null && partySize > discount.getMaxPartySize()) {
            throw new InvalidOperationException(
                "Maximum party size is " + discount.getMaxPartySize()
            );
        }
    }

    private BigDecimal calculateBillTierDiscount(Discount discount, BigDecimal orderTotal) {
        String tierConfig = discount.getTierConfig();
        if (tierConfig == null || tierConfig.isEmpty()) {
            throw new InvalidOperationException("Bill tier discount requires tier configuration");
        }

        try {
            JsonNode config = objectMapper.readTree(tierConfig);
            
            final BigDecimal[] highestDiscount = {BigDecimal.ZERO};
            
            config.fields().forEachRemaining(entry -> {
                JsonNode tierData = entry.getValue();
                BigDecimal minAmount = BigDecimal.valueOf(tierData.get("min").asDouble());
                
                if (orderTotal.compareTo(minAmount) >= 0) {
                    BigDecimal tierDiscount = BigDecimal.valueOf(tierData.get("discount").asDouble());
                    
                    BigDecimal discountAmount;
                    if (tierDiscount.compareTo(BigDecimal.valueOf(100)) <= 0) {
                        discountAmount = orderTotal
                                .multiply(tierDiscount)
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    } else {
                        discountAmount = tierDiscount;
                    }
                    
                    if (discountAmount.compareTo(highestDiscount[0]) > 0) {
                        highestDiscount[0] = discountAmount;
                    }
                }
            });
            
            return highestDiscount[0];
            
        } catch (Exception e) {
            log.error("Error parsing tier config: {}", e.getMessage());
            throw new InvalidOperationException("Invalid tier configuration");
        }
    }

    public boolean isDiscountApplicableToItem(Discount discount, Long itemId) {
        if (!discount.getApplyToSpecificItems()) {
            return true;
        }

        return discount.getItems().stream()
                .anyMatch(item -> item.getId().equals(itemId));
    }
}
