package com.restaurant.qrorder.domain.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

import static lombok.AccessLevel.PRIVATE;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
public class UpdateItemRequest {

    @Size(max = 200, message = "Item name must not exceed 200 characters")
    String name;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    BigDecimal price;

    @Size(max = 50, message = "Unit must not exceed 50 characters")
    String unit;

    Long categoryId;

    String description;

    String imageUrl;

    Boolean available;
}
