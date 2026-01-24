package com.restaurant.qrorder.domain.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
public class ApplyDiscountRequest {

    @NotNull(message = "Discount ID is required")
    Long discountId;

    @NotEmpty(message = "Item IDs cannot be empty")
    List<Long> itemIds;
}
