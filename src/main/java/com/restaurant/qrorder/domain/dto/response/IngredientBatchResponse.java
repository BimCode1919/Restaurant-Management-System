package com.restaurant.qrorder.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngredientBatchResponse {

    private Long id;
    private Long ingredientId;
    private String ingredientName;
    private BigDecimal quantity;
    private LocalDateTime expiryDate;
    private LocalDateTime createdAt;
}
