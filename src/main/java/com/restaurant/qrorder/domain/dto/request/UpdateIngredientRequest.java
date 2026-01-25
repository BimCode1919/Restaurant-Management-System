package com.restaurant.qrorder.domain.dto.request;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateIngredientRequest {

    private String name;

    @DecimalMin(value = "0.0", message = "Stock quantity must be >= 0")
    private BigDecimal stockQuantity;

    private String unit;
}
