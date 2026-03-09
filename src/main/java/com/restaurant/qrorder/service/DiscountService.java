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

    // ==================== CRUD METHODS ====================

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

        if (!discount.getActive()) {
            throw new IllegalStateException("Discount code is not active");
        }
        if (discount.getStartDate() != null && now.isBefore(discount.getStartDate())) {
            throw new IllegalStateException("Discount is not yet valid");
        }
        if (discount.getEndDate() != null && now.isAfter(discount.getEndDate())) {
            throw new IllegalStateException("Discount has expired");
        }
        if (discount.getUsageLimit() != null && discount.getUsedCount() >= discount.getUsageLimit()) {
            throw new IllegalStateException("Discount usage limit reached");
        }
        if (orderAmount != null && discount.getMinOrderAmount() != null) {
            if (BigDecimal.valueOf(orderAmount).compareTo(discount.getMinOrderAmount()) < 0) {
                throw new IllegalStateException("Order amount does not meet minimum requirement of " + discount.getMinOrderAmount());
            }
        }
        if (partySize != null) {
            if (discount.getMinPartySize() != null && partySize < discount.getMinPartySize()) {
                throw new IllegalStateException("Party size does not meet minimum requirement of " + discount.getMinPartySize());
            }
            if (discount.getMaxPartySize() != null && partySize > discount.getMaxPartySize()) {
                throw new IllegalStateException("Party size exceeds maximum limit of " + discount.getMaxPartySize());
            }
        }
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
        Discount savedDiscount = discountRepository.save(discountMapper.toEntity(request));
        log.info("Discount created successfully with id: {}", savedDiscount.getId());
        return discountMapper.toResponse(savedDiscount);
    }

    @Transactional
    public DiscountResponse updateDiscount(Long id, UpdateDiscountRequest request) {
        log.debug("Updating discount with id: {}", id);

        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found with id: " + id));

        if (request.getName() != null) discount.setName(request.getName());
        if (request.getDescription() != null) discount.setDescription(request.getDescription());
        if (request.getDiscountType() != null) discount.setDiscountType(request.getDiscountType());
        if (request.getValue() != null) discount.setValue(request.getValue());
        if (request.getMinOrderAmount() != null) discount.setMinOrderAmount(request.getMinOrderAmount());
        if (request.getMaxDiscountAmount() != null) discount.setMaxDiscountAmount(request.getMaxDiscountAmount());
        if (request.getStartDate() != null) discount.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) discount.setEndDate(request.getEndDate());
        if (request.getUsageLimit() != null) discount.setUsageLimit(request.getUsageLimit());
        if (request.getMinPartySize() != null) discount.setMinPartySize(request.getMinPartySize());
        if (request.getMaxPartySize() != null) discount.setMaxPartySize(request.getMaxPartySize());
        if (request.getTierConfig() != null) discount.setTierConfig(request.getTierConfig());
        if (request.getApplicableDays() != null) discount.setApplicableDays(request.getApplicableDays());
        if (request.getApplyToSpecificItems() != null) discount.setApplyToSpecificItems(request.getApplyToSpecificItems());
        if (request.getActive() != null) discount.setActive(request.getActive());

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
     * Finds applicable discounts, picks the best one, applies it to the bill,
     * and manages usage counts for both the previous and new discount.
     */
    @Transactional
    public DiscountCalculationResult applyBestDiscountToBill(Bill bill) {
        log.debug("Applying best discount to bill ID: {}", bill.getId());

        List<Discount> applicableDiscounts = getApplicableDiscounts(bill);

        if (applicableDiscounts.isEmpty()) {
            log.info("No applicable discounts found for bill ID: {}", bill.getId());
            bill.setDiscount(null);
            bill.setDiscountAmount(BigDecimal.ZERO);
            bill.setFinalPrice(bill.getTotalPrice());
            return DiscountCalculationResult.noDiscount();
        }

        // Pick the discount that gives the highest saving — entity already in memory, no extra DB fetch needed
        Discount bestDiscount = applicableDiscounts.stream()
                .max(Comparator.comparing(d -> calculateDiscountAmount(d, bill).getDiscountAmount()))
                .orElseThrow();

        DiscountCalculationResult bestResult = calculateDiscountAmount(bestDiscount, bill);

        // Decrement previous discount usage if it's being replaced
        if (bill.getDiscount() != null && !bill.getDiscount().getId().equals(bestDiscount.getId())) {
            Discount previousDiscount = bill.getDiscount();
            previousDiscount.setUsedCount(Math.max(0, previousDiscount.getUsedCount() - 1));
            discountRepository.save(previousDiscount);
            log.info("Decremented usage count for previous discount [{}]", previousDiscount.getId());
        }

        // Increment usage count only if this discount wasn't already applied to this bill
        boolean isNewDiscount = bill.getDiscount() == null
                || !bill.getDiscount().getId().equals(bestDiscount.getId());
        if (isNewDiscount) {
            bestDiscount.setUsedCount(bestDiscount.getUsedCount() + 1);
            discountRepository.save(bestDiscount);
        }

        bill.setDiscount(bestDiscount);
        bill.setDiscountAmount(bestResult.getDiscountAmount());
        bill.setFinalPrice(bill.getTotalPrice().subtract(bestResult.getDiscountAmount()));

        log.info("Applied best discount [{}] to bill [{}]: discountAmount={}, finalPrice={}",
                bestDiscount.getName(), bill.getId(),
                bestResult.getDiscountAmount(), bestResult.getFinalAmount());

        return bestResult;
    }

    /**
     * Apply a specific discount to a bill by ID.
     */
    @Transactional
    public void applyDiscountToBill(Bill bill, Long discountId) {
        Discount discount = discountRepository.findById(discountId)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found with id: " + discountId));

        if (!discount.getActive()) {
            throw new IllegalStateException("Discount is not active");
        }

        LocalDateTime now = LocalDateTime.now();

        if (discount.getStartDate() != null && now.isBefore(discount.getStartDate())) {
            throw new IllegalStateException("Discount is not yet valid");
        }
        if (discount.getEndDate() != null && now.isAfter(discount.getEndDate())) {
            throw new IllegalStateException("Discount has expired");
        }
        if (discount.getUsageLimit() != null && discount.getUsedCount() >= discount.getUsageLimit()) {
            throw new IllegalStateException("Discount usage limit reached");
        }
        if (discount.getMinOrderAmount() != null
                && bill.getTotalPrice().compareTo(discount.getMinOrderAmount()) < 0) {
            throw new IllegalStateException("Order amount does not meet minimum requirement");
        }
        if (discount.getMinPartySize() != null
                && (bill.getPartySize() == null || bill.getPartySize() < discount.getMinPartySize())) {
            throw new IllegalStateException("Party size does not meet minimum requirement");
        }
        if (discount.getApplicableDays() != null && !discount.getApplicableDays().isEmpty()
                && !isApplicableDay(discount.getApplicableDays(), now)) {
            throw new IllegalStateException("Discount is not applicable today");
        }

        DiscountCalculationResult result = calculateDiscountAmount(discount, bill);

        bill.setDiscount(discount);
        bill.setDiscountAmount(result.getDiscountAmount());
        bill.setFinalPrice(bill.getTotalPrice().subtract(result.getDiscountAmount()));

        discount.setUsedCount(discount.getUsedCount() + 1);
        discountRepository.save(discount);

        log.info("Applied discount {} to bill {}: Amount = {}",
                discount.getName(), bill.getId(), result.getDiscountAmount());
    }

    /**
     * Calculate the best discount for a bill without applying it (read-only).
     */
    public DiscountCalculationResult calculateBillDiscount(Bill bill) {
        log.debug("Calculating discount for bill ID: {}", bill.getId());

        List<Discount> applicableDiscounts = getApplicableDiscounts(bill);

        if (applicableDiscounts.isEmpty()) {
            log.debug("No applicable discounts found for bill ID: {}", bill.getId());
            return DiscountCalculationResult.noDiscount();
        }

        DiscountCalculationResult best = applicableDiscounts.stream()
                .map(discount -> calculateDiscountAmount(discount, bill))
                .max(Comparator.comparing(DiscountCalculationResult::getDiscountAmount))
                .orElse(DiscountCalculationResult.noDiscount());

        log.info("Best discount for bill ID {}: {} with amount: {}",
                bill.getId(), best.getDiscountName(), best.getDiscountAmount());

        return best;
    }

    /**
     * Find best discount response for a bill without applying it (read-only).
     */
    @Transactional(readOnly = true)
    public DiscountResponse findBestDiscount(Bill bill) {
        log.debug("Finding best discount for bill ID: {}", bill.getId());

        return getApplicableDiscounts(bill).stream()
                .map(discount -> {
                    DiscountCalculationResult result = calculateDiscountAmount(discount, bill);
                    DiscountResponse response = discountMapper.toResponse(discount);
                    response.setCalculatedAmount(result.getDiscountAmount());
                    return response;
                })
                .max(Comparator.comparing(DiscountResponse::getCalculatedAmount))
                .orElse(null);
    }

    // ==================== PRIVATE HELPERS ====================

    private List<Discount> getApplicableDiscounts(Bill bill) {
        LocalDateTime now = LocalDateTime.now();
        return discountRepository.findActiveDiscounts(now).stream()
                .filter(discount -> isDiscountApplicable(discount, bill, now))
                .collect(Collectors.toList());
    }

    private boolean isDiscountApplicable(Discount discount, Bill bill, LocalDateTime now) {
        if (discount.getStartDate() != null && now.isBefore(discount.getStartDate())) return false;
        if (discount.getEndDate() != null && now.isAfter(discount.getEndDate())) return false;
        if (discount.getUsageLimit() != null && discount.getUsedCount() >= discount.getUsageLimit()) return false;
        if (discount.getMinOrderAmount() != null
                && bill.getTotalPrice().compareTo(discount.getMinOrderAmount()) < 0) return false;
        if (discount.getMinPartySize() != null
                && (bill.getPartySize() == null || bill.getPartySize() < discount.getMinPartySize())) return false;
        if (discount.getMaxPartySize() != null
                && (bill.getPartySize() == null || bill.getPartySize() > discount.getMaxPartySize())) return false;
        if (discount.getApplicableDays() != null && !discount.getApplicableDays().isEmpty()
                && !isApplicableDay(discount.getApplicableDays(), now)) return false;

        switch (discount.getDiscountType()) {
            case ITEM_SPECIFIC:
                return hasApplicableItems(discount, bill);
            case PARTY_SIZE:
                return bill.getPartySize() != null
                        && bill.getPartySize() >= (discount.getMinPartySize() != null ? discount.getMinPartySize() : 1);
            case BILL_TIER:
                return bill.getTotalPrice().compareTo(
                        discount.getMinOrderAmount() != null ? discount.getMinOrderAmount() : BigDecimal.ZERO) >= 0;
            case HOLIDAY:
                // Applicable days already checked above
                return true;
            default:
                return true;
        }
    }

    /**
     * Fixed: uses Set.contains() instead of String.contains() to avoid partial matches
     * e.g. "MONDAY" no longer falsely matches "MONDAY_SPECIAL"
     */
    private boolean isApplicableDay(String applicableDays, LocalDateTime dateTime) {
        if (applicableDays == null || applicableDays.isEmpty()) {
            return true;
        }
        Set<String> days = Arrays.stream(applicableDays.toUpperCase().split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
        return days.contains(dateTime.getDayOfWeek().toString());
    }

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

    public DiscountCalculationResult calculateDiscountAmount(Discount discount, Bill bill) {
        BigDecimal totalPrice = bill.getTotalPrice();
        BigDecimal discountAmount;

        switch (discount.getDiscountType()) {
            case ITEM_SPECIFIC:
                discountAmount = calculateItemSpecificDiscount(discount, bill);
                break;
            case HOLIDAY:
                // Holiday discounts apply a percentage to the whole bill
                discountAmount = calculatePercentageDiscount(discount.getValue(), totalPrice);
                break;
            case PARTY_SIZE:
                discountAmount = calculatePercentageDiscount(discount.getValue(), totalPrice);
                break;
            case BILL_TIER:
                discountAmount = calculateTierDiscount(discount, totalPrice);
                break;
            default:
                discountAmount = calculatePercentageDiscount(discount.getValue(), totalPrice);
                break;
        }

        // Cap at max discount amount if configured
        if (discount.getMaxDiscountAmount() != null
                && discountAmount.compareTo(discount.getMaxDiscountAmount()) > 0) {
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
                        totalDiscount = totalDiscount.add(
                                discount.getValue().multiply(BigDecimal.valueOf(detail.getQuantity())));
                    }
                }
            }
        }

        return totalDiscount;
    }

    private BigDecimal calculatePercentageDiscount(BigDecimal percentage, BigDecimal amount) {
        return amount.multiply(percentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTierDiscount(Discount discount, BigDecimal totalPrice) {
        if (discount.getTierConfig() == null || discount.getTierConfig().isEmpty()) {
            return calculatePercentageDiscount(discount.getValue(), totalPrice);
        }


        String cleanConfig = discount.getTierConfig().replace("\"", "").trim();

        BigDecimal applicablePercent = BigDecimal.ZERO;
        for (String tier : cleanConfig.split(",")) {
            String[] parts = tier.split(":");
            if (parts.length == 2) {
                try {
                    BigDecimal minAmount = new BigDecimal(parts[0].trim());
                    BigDecimal discountPercent = new BigDecimal(parts[1].trim());
                    if (totalPrice.compareTo(minAmount) >= 0) {
                        applicablePercent = discountPercent;
                    }
                } catch (NumberFormatException e) {
                    log.warn("Skipping malformed tier entry '{}' for discount [{}]: {}",
                            tier, discount.getId(), e.getMessage());
                }
            }
        }

        return calculatePercentageDiscount(applicablePercent, totalPrice);
    }
    // ==================== HELPER CLASS ====================

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