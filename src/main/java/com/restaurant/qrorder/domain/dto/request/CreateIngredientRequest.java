package com.restaurant.qrorder.domain.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateIngredientRequest {

    @NotBlank(message = "Ingredient name is required")
    private String name;

    @NotNull(message = "Stock quantity is required")
    @DecimalMin(value = "0.0", message = "Stock quantity must be >= 0")
    private BigDecimal stockQuantity;

    @NotBlank(message = "Unit is required")
    private String unit;
}
