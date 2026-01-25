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
public class IngredientResponse {

    private Long id;
    private String name;
    private BigDecimal stockQuantity;
    private String unit;
    private LocalDateTime updatedAt;
}
