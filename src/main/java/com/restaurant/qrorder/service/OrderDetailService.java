package com.restaurant.qrorder.service;


import com.restaurant.qrorder.domain.common.ItemStatus;
import com.restaurant.qrorder.domain.dto.response.OrderDetailResponse;
import com.restaurant.qrorder.domain.entity.*;
import com.restaurant.qrorder.repository.IngredientRepository;
import com.restaurant.qrorder.repository.IngredientUsageRepository;
import com.restaurant.qrorder.domain.entity.OrderDetail;
import com.restaurant.qrorder.util.OrderDetailSpecification;
import com.restaurant.qrorder.repository.OrderDetailRepository;
import com.restaurant.qrorder.repository.RecipeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderDetailService {

    private final OrderDetailRepository orderDetailRepository;
    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final IngredientUsageRepository ingredientUsageRepository;
    private Specification<OrderDetail> specification;


    @Transactional
    public OrderDetailResponse updateItemStatus(Long orderDetailId, ItemStatus request) {

        OrderDetail orderDetail = orderDetailRepository.findById(orderDetailId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "OrderDetail not found with id: " + orderDetailId));

//        validateTransition(orderDetail.getItemStatus(), request.getItemStatus());

        orderDetail.setItemStatus(request);

        if (request == ItemStatus.SERVED && orderDetail.getItemStatus() != ItemStatus.SERVED) {
            deductIngredients(orderDetail);
        }

        OrderDetail saved = orderDetailRepository.save(orderDetail);
        log.info("OrderDetail ID: {} status updated: {} → {}",
                saved.getId(), orderDetail.getItemStatus(), request);

        return mapToResponse(saved);
    }

    private void deductIngredients(OrderDetail orderDetail) {
        Item item = orderDetail.getItem();
        int quantity = orderDetail.getQuantity();

        // Get all recipes for this item
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

                // ✅ Check if low stock
                if (ingredient.getStockQuantity().compareTo(quantityNeeded) < 0) {
                    log.warn("Low stock for '{}': need {} but only {} available",
                            ingredient.getName(), quantityNeeded, ingredient.getStockQuantity());
                    quantityNeeded = ingredient.getStockQuantity(); // deduct whatever is left
                    wasLowStock  = true;
                }

                IngredientUsage usage = IngredientUsage.builder()
                        .orderDetail(orderDetail)
                        .ingredient(ingredient)  // ✅ direct link
                        .quantityUsed(quantityNeeded)
                        .unit(ingredient.getUnit())
                        .build();
                ingredientUsageRepository.save(usage);

                // ✅ Deduct from ingredient stock
                ingredient.setStockQuantity(
                        ingredient.getStockQuantity().subtract(quantityNeeded));
                ingredientRepository.save(ingredient);

                log.info("IngredientUsage saved — ingredient: '{}', used: {}/{} {}, lowStock: {}",
                        ingredient.getName(), quantityNeeded, quantityNeeded,
                        recipe.getUnit(), wasLowStock);
            }
    }

    private OrderDetailResponse mapToResponse(OrderDetail od) {
        return OrderDetailResponse.builder()
                .id(od.getId())
                .itemId(od.getItem().getId())
                .itemName(od.getItem().getName())
                .quantity(od.getQuantity())
                .itemStatus(od.getItemStatus())
                .notes(od.getNote())
                .price(od.getPrice())
                .build();
    }

    @Transactional(readOnly = true)
    public List<OrderDetailResponse> getOrderDetailsByStatus(ItemStatus status) {
        specification = new OrderDetailSpecification().getItemByStatus(status);
        return orderDetailRepository.findAll(specification).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

}
