package com.restaurant.qrorder.service;

import com.restaurant.qrorder.domain.common.DiscountType;
import com.restaurant.qrorder.domain.common.DiscountValueType;
import com.restaurant.qrorder.domain.dto.request.CreateDiscountRequest;
import com.restaurant.qrorder.domain.dto.request.UpdateDiscountRequest;
import com.restaurant.qrorder.domain.dto.response.DiscountResponse;
import com.restaurant.qrorder.domain.entity.Bill;
import com.restaurant.qrorder.domain.entity.Discount;
import com.restaurant.qrorder.domain.entity.Item;
import com.restaurant.qrorder.domain.entity.OrderDetail;
import com.restaurant.qrorder.exception.custom.ResourceNotFoundException;
import com.restaurant.qrorder.mapper.DiscountMapper;
import com.restaurant.qrorder.repository.DiscountRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class DiscountService {

    DiscountRepository discountRepository;
    DiscountMapper discountMapper;

    @Transactional(readOnly = true)
    public List<DiscountResponse> getAllDiscounts() {
        log.debug("Fetching all discounts");
        return discountRepository.findAll().stream()
                .map(discountMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DiscountResponse> getActiveDiscounts() {
        log.debug("Fetching active discounts");
        return discountRepository.findActiveDiscounts(LocalDateTime.now()).stream()
                .map(discountMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DiscountResponse getDiscountById(Long id) {
        log.debug("Fetching discount by id: {}", id);
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found with id: " + id));
        return discountMapper.toResponse(discount);
    }

    @Transactional(readOnly = true)
    public DiscountResponse getDiscountByCode(String code) {
        log.debug("Fetching discount by code: {}", code);
        Discount discount = discountRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found with code: " + code));
        return discountMapper.toResponse(discount);
    }

    @Transactional(readOnly = true)
    public DiscountResponse validateDiscountCode(String code, Double orderAmount, Integer partySize) {
        log.debug("Validating discount code: {} for order amount: {}, party size: {}", code, orderAmount, partySize);
        
        Discount discount = discountRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid discount code: " + code));
        
        LocalDateTime now = LocalDateTime.now();
        
        // Check if discount is active
        if (!discount.getActive()) {
            throw new IllegalStateException("Discount code is not active");
        }
        
        // Check date range
        if (discount.getStartDate() != null && now.isBefore(discount.getStartDate())) {
            throw new IllegalStateException("Discount is not yet valid");
        }
        if (discount.getEndDate() != null && now.isAfter(discount.getEndDate())) {
            throw new IllegalStateException("Discount has expired");
        }
        
        // Check usage limit
        if (discount.getUsageLimit() != null && discount.getUsedCount() >= discount.getUsageLimit()) {
            throw new IllegalStateException("Discount usage limit reached");
        }
        
        // Check minimum order amount if provided
        if (orderAmount != null && discount.getMinOrderAmount() != null) {
            if (BigDecimal.valueOf(orderAmount).compareTo(discount.getMinOrderAmount()) < 0) {
                throw new IllegalStateException("Order amount does not meet minimum requirement of " + discount.getMinOrderAmount());
            }
        }
        
        // Check party size if provided
        if (partySize != null) {
            if (discount.getMinPartySize() != null && partySize < discount.getMinPartySize()) {
                throw new IllegalStateException("Party size does not meet minimum requirement of " + discount.getMinPartySize());
            }
            if (discount.getMaxPartySize() != null && partySize > discount.getMaxPartySize()) {
                throw new IllegalStateException("Party size exceeds maximum limit of " + discount.getMaxPartySize());
            }
        }
        
        // Check applicable days
        if (discount.getApplicableDays() != null && !discount.getApplicableDays().isEmpty()) {
            if (!isApplicableDay(discount.getApplicableDays(), now)) {
                throw new IllegalStateException("Discount is not applicable on " + now.getDayOfWeek());
            }
        }
        
        log.info("Discount code {} validated successfully", code);
        return discountMapper.toResponse(discount);
    }

    @Transactional
    public DiscountResponse createDiscount(CreateDiscountRequest request) {
        log.debug("Creating new discount: {}", request.getName());

        Discount discount = discountMapper.toEntity(request);
        Discount savedDiscount = discountRepository.save(discount);

        log.info("Discount created successfully with id: {}", savedDiscount.getId());
        return discountMapper.toResponse(savedDiscount);
    }

    @Transactional
    public DiscountResponse updateDiscount(Long id, UpdateDiscountRequest request) {
        log.debug("Updating discount with id: {}", id);

        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found with id: " + id));

        if (request.getName() != null) {
            discount.setName(request.getName());
        }

        if (request.getDescription() != null) {
            discount.setDescription(request.getDescription());
        }

        if (request.getDiscountType() != null) {
            discount.setDiscountType(request.getDiscountType());
        }

        if (request.getValue() != null) {
            discount.setValue(request.getValue());
        }

        if (request.getMinOrderAmount() != null) {
            discount.setMinOrderAmount(request.getMinOrderAmount());
        }

        if (request.getMaxDiscountAmount() != null) {
            discount.setMaxDiscountAmount(request.getMaxDiscountAmount());
        }

        if (request.getStartDate() != null) {
            discount.setStartDate(request.getStartDate());
        }

        if (request.getEndDate() != null) {
            discount.setEndDate(request.getEndDate());
        }

        if (request.getUsageLimit() != null) {
            discount.setUsageLimit(request.getUsageLimit());
        }

        // Update advanced fields
        if (request.getMinPartySize() != null) {
            discount.setMinPartySize(request.getMinPartySize());
        }

        if (request.getMaxPartySize() != null) {
            discount.setMaxPartySize(request.getMaxPartySize());
        }

        if (request.getTierConfig() != null) {
            discount.setTierConfig(request.getTierConfig());
        }

        if (request.getApplicableDays() != null) {
            discount.setApplicableDays(request.getApplicableDays());
        }

        if (request.getApplyToSpecificItems() != null) {
            discount.setApplyToSpecificItems(request.getApplyToSpecificItems());
        }

        if (request.getActive() != null) {
            discount.setActive(request.getActive());
        }

        Discount updatedDiscount = discountRepository.save(discount);
        log.info("Discount updated successfully with id: {}", id);

        return discountMapper.toResponse(updatedDiscount);
    }

    @Transactional
    public void deleteDiscount(Long id) {
        log.debug("Deleting discount with id: {}", id);

        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found with id: " + id));

        discountRepository.delete(discount);
        log.info("Discount deleted successfully with id: {}", id);
    }

    @Transactional
    public DiscountResponse toggleDiscountStatus(Long id) {
        log.debug("Toggling discount status with id: {}", id);

        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found with id: " + id));

        discount.setActive(!discount.getActive());
        Discount updatedDiscount = discountRepository.save(discount);

        log.info("Discount status toggled to {} for id: {}", updatedDiscount.getActive(), id);
        return discountMapper.toResponse(updatedDiscount);
    }

    // ==================== DISCOUNT CALCULATION METHODS ====================

    /**
     * Calculate discount for a bill
     * @param bill The bill to calculate discount for
     * @return DiscountCalculationResult with discount details
     */
    public DiscountCalculationResult calculateBillDiscount(Bill bill) {
        log.debug("Calculating discount for bill ID: {}", bill.getId());

        // Get all applicable discounts
        List<Discount> applicableDiscounts = getApplicableDiscounts(bill);

        if (applicableDiscounts.isEmpty()) {
            log.debug("No applicable discounts found for bill ID: {}", bill.getId());
            return DiscountCalculationResult.noDiscount();
        }

        // Find best discount
        DiscountCalculationResult bestDiscount = applicableDiscounts.stream()
                .map(discount -> calculateDiscountAmount(discount, bill))
                .max(Comparator.comparing(DiscountCalculationResult::getDiscountAmount))
                .orElse(DiscountCalculationResult.noDiscount());

        log.info("Best discount for bill ID {}: {} with amount: {}", 
                bill.getId(), 
                bestDiscount.getDiscountName(), 
                bestDiscount.getDiscountAmount());

        return bestDiscount;
    }

    /**
     * Get all applicable discounts for a bill
     */
    private List<Discount> getApplicableDiscounts(Bill bill) {
        LocalDateTime now = LocalDateTime.now();
        List<Discount> activeDiscounts = discountRepository.findActiveDiscounts(now);

        return activeDiscounts.stream()
                .filter(discount -> isDiscountApplicable(discount, bill, now))
                .collect(Collectors.toList());
    }

    /**
     * Check if discount is applicable to the bill
     */
    private boolean isDiscountApplicable(Discount discount, Bill bill, LocalDateTime now) {
        // Check date range
        if (discount.getStartDate() != null && now.isBefore(discount.getStartDate())) {
            return false;
        }
        if (discount.getEndDate() != null && now.isAfter(discount.getEndDate())) {
            return false;
        }

        // Check usage limit
        if (discount.getUsageLimit() != null && discount.getUsedCount() >= discount.getUsageLimit()) {
            return false;
        }

        // Check minimum order amount
        if (discount.getMinOrderAmount() != null && 
            bill.getTotalPrice().compareTo(discount.getMinOrderAmount()) < 0) {
            return false;
        }

        // Check party size
        if (discount.getMinPartySize() != null && 
            (bill.getPartySize() == null || bill.getPartySize() < discount.getMinPartySize())) {
            return false;
        }
        if (discount.getMaxPartySize() != null && 
            (bill.getPartySize() == null || bill.getPartySize() > discount.getMaxPartySize())) {
            return false;
        }

        // Check applicable days
        if (discount.getApplicableDays() != null && !discount.getApplicableDays().isEmpty()) {
            if (!isApplicableDay(discount.getApplicableDays(), now)) {
                return false;
            }
        }

        // Type-specific checks
        switch (discount.getDiscountType()) {
            case ITEM_SPECIFIC:
                return hasApplicableItems(discount, bill);
            case HOLIDAY:
                return isApplicableDay(discount.getApplicableDays(), now);
            case PARTY_SIZE:
                return bill.getPartySize() != null && 
                       bill.getPartySize() >= (discount.getMinPartySize() != null ? discount.getMinPartySize() : 1);
            case BILL_TIER:
                return bill.getTotalPrice().compareTo(discount.getMinOrderAmount() != null ? 
                       discount.getMinOrderAmount() : BigDecimal.ZERO) >= 0;
            default:
                return true;
        }
    }

    /**
     * Check if current day is applicable
     */
    private boolean isApplicableDay(String applicableDays, LocalDateTime dateTime) {
        if (applicableDays == null || applicableDays.isEmpty()) {
            return true;
        }

        DayOfWeek currentDay = dateTime.getDayOfWeek();
        String dayName = currentDay.toString();
        
        // applicableDays format: "MONDAY,FRIDAY,SATURDAY"
        return applicableDays.toUpperCase().contains(dayName);
    }

    /**
     * Check if bill has items eligible for item-specific discount
     */
    private boolean hasApplicableItems(Discount discount, Bill bill) {
        if (!discount.getApplyToSpecificItems()) {
            return true;
        }

        Set<Long> discountItemIds = discount.getItems().stream()
                .map(Item::getId)
                .collect(Collectors.toSet());

        return bill.getOrders().stream()
                .flatMap(order -> order.getOrderDetails().stream())
                .anyMatch(detail -> discountItemIds.contains(detail.getItem().getId()));
    }

    /**
     * Calculate discount amount for a specific discount
     */
    public DiscountCalculationResult calculateDiscountAmount(Discount discount, Bill bill) {
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal totalPrice = bill.getTotalPrice();

        switch (discount.getDiscountType()) {
            case ITEM_SPECIFIC:
                discountAmount = calculateItemSpecificDiscount(discount, bill);
                break;
            
            case HOLIDAY:
            case PARTY_SIZE:
                discountAmount = calculatePercentageDiscount(discount.getValue(), totalPrice);
                break;
            
            case BILL_TIER:
                discountAmount = calculateTierDiscount(discount, totalPrice);
                break;
        }

        // Apply max discount limit
        if (discount.getMaxDiscountAmount() != null && 
            discountAmount.compareTo(discount.getMaxDiscountAmount()) > 0) {
            discountAmount = discount.getMaxDiscountAmount();
        }

        return DiscountCalculationResult.builder()
                .discountId(discount.getId())
                .discountName(discount.getName())
                .discountType(discount.getDiscountType())
                .discountAmount(discountAmount)
                .originalAmount(totalPrice)
                .finalAmount(totalPrice.subtract(discountAmount))
                .build();
    }

    /**
     * Calculate item-specific discount
     */
    private BigDecimal calculateItemSpecificDiscount(Discount discount, Bill bill) {
        BigDecimal totalDiscount = BigDecimal.ZERO;

        Set<Long> discountItemIds = discount.getItems().stream()
                .map(Item::getId)
                .collect(Collectors.toSet());

        for (var order : bill.getOrders()) {
            for (OrderDetail detail : order.getOrderDetails()) {
                if (discountItemIds.contains(detail.getItem().getId())) {
                    BigDecimal itemTotal = detail.getPrice().multiply(BigDecimal.valueOf(detail.getQuantity()));
                    
                    if (discount.getValueType() == DiscountValueType.PERCENTAGE) {
                        totalDiscount = totalDiscount.add(calculatePercentageDiscount(discount.getValue(), itemTotal));
                    } else if (discount.getValueType() == DiscountValueType.FIXED_AMOUNT) {
                        totalDiscount = totalDiscount.add(discount.getValue().multiply(BigDecimal.valueOf(detail.getQuantity())));
                    }
                }
            }
        }

        return totalDiscount;
    }

    /**
     * Calculate percentage discount
     */
    private BigDecimal calculatePercentageDiscount(BigDecimal percentage, BigDecimal amount) {
        return amount.multiply(percentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate tier-based discount
     */
    private BigDecimal calculateTierDiscount(Discount discount, BigDecimal totalPrice) {
        if (discount.getTierConfig() == null || discount.getTierConfig().isEmpty()) {
            // Fallback to simple percentage
            return calculatePercentageDiscount(discount.getValue(), totalPrice);
        }

        // Parse tier config: "500000:5,1000000:10,2000000:15"
        // Format: "minAmount:discountPercent,..."
        String[] tiers = discount.getTierConfig().split(",");
        BigDecimal applicableDiscount = BigDecimal.ZERO;

        for (String tier : tiers) {
            String[] parts = tier.split(":");
            if (parts.length == 2) {
                BigDecimal minAmount = new BigDecimal(parts[0].trim());
                BigDecimal discountPercent = new BigDecimal(parts[1].trim());

                if (totalPrice.compareTo(minAmount) >= 0) {
                    applicableDiscount = discountPercent;
                }
            }
        }

        return calculatePercentageDiscount(applicableDiscount, totalPrice);
    }

    /**
     * Apply discount to bill
     */
    @Transactional
    public void applyDiscountToBill(Bill bill, Long discountId) {
        Discount discount = discountRepository.findById(discountId)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found with id: " + discountId));

        if (!discount.getActive()) {
            throw new IllegalStateException("Discount is not active");
        }

        DiscountCalculationResult result = calculateDiscountAmount(discount, bill);

        bill.setDiscount(discount);
        bill.setDiscountAmount(result.getDiscountAmount());
        bill.setFinalPrice(bill.getTotalPrice().subtract(result.getDiscountAmount()));

        // Increment usage count
        discount.setUsedCount(discount.getUsedCount() + 1);
        discountRepository.save(discount);

        log.info("Applied discount {} to bill {}: Amount = {}", 
                discount.getName(), bill.getId(), result.getDiscountAmount());
    }

    /**
     * Find best discount for bill
     */
    public DiscountResponse findBestDiscount(Bill bill) {
        DiscountCalculationResult result = calculateBillDiscount(bill);
        
        if (result.getDiscountId() == null) {
            return null;
        }

        Discount discount = discountRepository.findById(result.getDiscountId())
                .orElse(null);

        if (discount == null) {
            return null;
        }

        DiscountResponse response = discountMapper.toResponse(discount);
        response.setCalculatedAmount(result.getDiscountAmount());
        return response;
    }

    // ==================== HELPER CLASSES ====================

    /**
     * Result of discount calculation
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DiscountCalculationResult {
        private Long discountId;
        private String discountName;
        private DiscountType discountType;
        private BigDecimal discountAmount;
        private BigDecimal originalAmount;
        private BigDecimal finalAmount;

        public static DiscountCalculationResult noDiscount() {
            return DiscountCalculationResult.builder()
                    .discountAmount(BigDecimal.ZERO)
                    .build();
        }
    }
}
