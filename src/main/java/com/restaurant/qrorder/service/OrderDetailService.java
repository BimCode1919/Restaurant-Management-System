package com.restaurant.qrorder.service;

import com.restaurant.qrorder.domain.common.ItemStatus;
import com.restaurant.qrorder.domain.dto.response.OrderDetailResponse;
import com.restaurant.qrorder.domain.entity.*;
import com.restaurant.qrorder.repository.IngredientRepository;
import com.restaurant.qrorder.repository.IngredientUsageRepository;
import com.restaurant.qrorder.repository.BillRepository;
import com.restaurant.qrorder.repository.OrderDetailRepository;
import com.restaurant.qrorder.repository.RecipeRepository;
import com.restaurant.qrorder.util.OrderDetailSpecification;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderDetailService {

    private final OrderDetailRepository orderDetailRepository;
    private final BillRepository billRepository;
    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final IngredientUsageRepository ingredientUsageRepository;

    @Transactional
    public OrderDetailResponse updateItemStatus(Long orderDetailId, ItemStatus request) {
        OrderDetail orderDetail = orderDetailRepository.findById(orderDetailId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "OrderDetail not found with id: " + orderDetailId));

        ItemStatus oldStatus = orderDetail.getItemStatus(); // ← capture BEFORE mutating

        if (request == ItemStatus.CANCELLED) {
            Bill bill = orderDetail.getOrder().getBill();
            bill.setTotalPrice(bill.getTotalPrice().subtract(
                    orderDetail.getPrice().multiply(BigDecimal.valueOf(orderDetail.getQuantity()))
            ));
            // Recalculate finalPrice so discount stays consistent
            bill.setFinalPrice(bill.getTotalPrice().subtract(bill.getDiscountAmount()));
            billRepository.save(bill);
        }

        if (request == ItemStatus.READY && oldStatus != ItemStatus.READY) {
            deductIngredients(orderDetail);
        }

        orderDetail.setItemStatus(request);
        OrderDetail saved = orderDetailRepository.save(orderDetail);

        log.info("OrderDetail ID: {} status updated: {} → {}", saved.getId(), oldStatus, request);

        return mapToResponse(saved);
    }

    private void deductIngredients(OrderDetail orderDetail) {
        Item item = orderDetail.getItem();
        int quantity = orderDetail.getQuantity();

        List<Recipe> recipes = recipeRepository.findByItemId(item.getId());

        if (recipes.isEmpty()) {
            log.warn("No recipe found for item ID: {} — skipping ingredient deduction", item.getId());
            return;
        }

        for (Recipe recipe : recipes) {
            Ingredient ingredient = recipe.getIngredient();

            BigDecimal quantityNeeded = recipe.getQuantity()
                    .multiply(BigDecimal.valueOf(quantity));

            boolean wasLowStock = false;

            if (ingredient.getStockQuantity().compareTo(quantityNeeded) < 0) {
                log.warn("Low stock for '{}': need {} but only {} available",
                        ingredient.getName(), quantityNeeded, ingredient.getStockQuantity());
                quantityNeeded = ingredient.getStockQuantity();
                wasLowStock = true;
            }

            IngredientUsage usage = IngredientUsage.builder()
                    .orderDetail(orderDetail)
                    .ingredient(ingredient)
                    .quantityUsed(quantityNeeded)
                    .unit(ingredient.getUnit())
                    .build();
            ingredientUsageRepository.save(usage);

            ingredient.setStockQuantity(
                    ingredient.getStockQuantity().subtract(quantityNeeded));
            ingredientRepository.save(ingredient);

            log.info("IngredientUsage saved — ingredient: '{}', used: {} {}, lowStock: {}",
                    ingredient.getName(), quantityNeeded, recipe.getUnit(), wasLowStock);
        }
    }

    private OrderDetailResponse mapToResponse(OrderDetail od) {
        return OrderDetailResponse.builder()
                .id(od.getId())
                .itemId(od.getItem().getId())
                .itemName(od.getItem().getName())
                .quantity(od.getQuantity())
                .price(od.getPrice())
                .subtotal(od.getPrice().multiply(BigDecimal.valueOf(od.getQuantity())))
                .itemStatus(od.getItemStatus())
                .notes(od.getNote())
                .build();
    }

    @Transactional(readOnly = true)
    public List<OrderDetailResponse> getOrderDetailsByStatus(ItemStatus status) {
        Specification<OrderDetail> spec = new OrderDetailSpecification().getItemByStatus(status);
        return orderDetailRepository.findAll(spec).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
}