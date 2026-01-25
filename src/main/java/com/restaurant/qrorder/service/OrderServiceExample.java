package com.restaurant.qrorder.service;

import com.restaurant.qrorder.domain.dto.request.CreateOrderRequest;
import com.restaurant.qrorder.domain.entity.Discount;
import com.restaurant.qrorder.repository.DiscountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * EXAMPLE: How to integrate DiscountCalculationService into OrderService
 * 
 * This is a sample implementation showing how to:
 * 1. Validate discount code
 * 2. Calculate discount amount
 * 3. Apply to order total
 * 4. Update discount usage count
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceExample {
    
    private final DiscountRepository discountRepository;
    private final DiscountCalculationService discountCalculationService;
    
    /**
     * Example method: Create order with auto-applied best discount
     */
    @Transactional
    public void createOrderWithDiscount(CreateOrderRequest request) {
        
        // Step 1: Calculate order total from items
        BigDecimal orderTotal = calculateOrderTotal(request);
        log.info("Order total calculated: {}", orderTotal);
        
        BigDecimal discountAmount = BigDecimal.ZERO;
        Long discountId = null;
        
        // Step 2: Auto-find and apply best discount
        Discount bestDiscount = findBestApplicableDiscount(orderTotal, request.getPartySize());
        
        if (bestDiscount != null) {
            log.info("Auto-applying discount: {} ({})", bestDiscount.getName(), bestDiscount.getDiscountType());
            
            // Calculate discount amount
            discountAmount = discountCalculationService.calculateDiscountAmount(
                bestDiscount,
                orderTotal,
                request.getPartySize(),
                LocalDateTime.now()
            );
            
            log.info("Discount calculated: {} (from total {})", discountAmount, orderTotal);
            
            // Increment usage count
            bestDiscount.setUsedCount(bestDiscount.getUsedCount() + 1);
            discountRepository.save(bestDiscount);
            
            discountId = bestDiscount.getId();
        } else {
            log.info("No applicable discount found for this order");
        }
        
        // Step 3: Calculate final amount
        BigDecimal finalAmount = orderTotal.subtract(discountAmount);
        
        log.info("Order summary - Total: {}, Discount: {}, Final: {}", 
            orderTotal, discountAmount, finalAmount);
        
        // Step 4: Save order with discount information
        // Order order = Order.builder()
        //     .tableId(request.getTableId())
        //     .totalAmount(orderTotal)
        //     .discountId(discountId)
        //     .discountAmount(discountAmount)
        //     .finalAmount(finalAmount)
        //     .partySize(request.getPartySize())
        //     .note(request.getNote())
        //     .status(OrderStatus.PENDING)
        //     .build();
        // orderRepository.save(order);
        
        // Step 5: Save order items
        // for (CreateOrderRequest.OrderItemRequest itemReq : request.getItems()) {
        //     OrderItem orderItem = OrderItem.builder()
        //         .orderId(order.getId())
        //         .itemId(itemReq.getItemId())
        //         .quantity(itemReq.getQuantity())
        //         .note(itemReq.getNote())
        //         .build();
        //     orderItemRepository.save(orderItem);
        // }
    }
    
    /**
     * Calculate order total from items
     */
    private BigDecimal calculateOrderTotal(CreateOrderRequest request) {
        // TODO: Fetch item prices and calculate total
        // This is just a placeholder
        return new BigDecimal("450000");
    }
    
    /**
     * Find the best applicable discount for the order
     * Returns the discount with highest discount amount
     */
    private Discount findBestApplicableDiscount(BigDecimal orderTotal, Integer partySize) {
        List<Discount> activeDiscounts = discountRepository.findActiveDiscounts(LocalDateTime.now());
        
        Discount bestDiscount = null;
        BigDecimal highestDiscountAmount = BigDecimal.ZERO;
        
        for (Discount discount : activeDiscounts) {
            try {
                BigDecimal amount = discountCalculationService.calculateDiscountAmount(
                    discount,
                    orderTotal,
                    partySize,
                    LocalDateTime.now()
                );
                
                if (amount.compareTo(highestDiscountAmount) > 0) {
                    highestDiscountAmount = amount;
                    bestDiscount = discount;
                }
            } catch (Exception e) {
                log.debug("Discount {} not applicable: {}", discount.getName(), e.getMessage());
            }
        }
        
        return bestDiscount;
    }
    
    /**
     * Example: Get all applicable discounts for current order
     */
    public void findApplicableDiscounts(BigDecimal orderTotal, Integer partySize) {
        
        // Get all active discounts
        var activeDiscounts = discountRepository.findActiveDiscounts(LocalDateTime.now());
        
        log.info("Found {} active discounts", activeDiscounts.size());
        
        // Filter applicable ones
        for (Discount discount : activeDiscounts) {
            try {
                // Try to calculate discount for each
                BigDecimal amount = discountCalculationService.calculateDiscountAmount(
                    discount,
                    orderTotal,
                    partySize,
                    LocalDateTime.now()
                );
                
                if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    log.info("Applicable: {} - Save {} ({}%)", 
                        discount.getName(), 
                        amount,
                        amount.multiply(BigDecimal.valueOf(100)).divide(orderTotal, 2, BigDecimal.ROUND_HALF_UP)
                    );
                }
                
            } catch (Exception e) {
                // Not applicable, skip
                log.debug("Discount {} not applicable: {}", discount.getName(), e.getMessage());
            }
        }
    }
}
