package com.restaurant.qrorder.domain.dto.request;

import com.restaurant.qrorder.util.MinFutureDate;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
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
public class CreateIngredientBatchRequest {

    @NotNull(message = "Ingredient ID is required")
    private Long ingredientId;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be greater than 0")
    @DecimalMax(value = "100.0", message = "Quantity must not exceed 100 ingredients per batch")
    private BigDecimal quantity;


    @MinFutureDate(minDays = 30)
    @NotNull(message = "Reservation time is required")
    @Future(message = "Expiry date must be in the future")
    private LocalDateTime expiryDate;
}
