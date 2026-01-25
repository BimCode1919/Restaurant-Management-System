package com.restaurant.qrorder.domain.dto.request;

import com.restaurant.qrorder.domain.common.DiscountType;
import com.restaurant.qrorder.domain.common.DiscountValueType;
import jakarta.validation.constraints.*;
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
public class CreateDiscountRequest {

    @NotBlank(message = "Discount name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    @NotNull(message = "Value type is required")
    @Builder.Default
    private DiscountValueType valueType = DiscountValueType.PERCENTAGE;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Discount value must be greater than 0")
    private BigDecimal value;

    @DecimalMin(value = "0.0", message = "Min order amount must be >= 0")
    private BigDecimal minOrderAmount;

    @DecimalMin(value = "0.0", message = "Max discount amount must be >= 0")
    private BigDecimal maxDiscountAmount;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @Min(value = 1, message = "Usage limit must be at least 1")
    private Integer usageLimit;

    @Min(value = 1, message = "Min party size must be at least 1")
    private Integer minPartySize;

    private Integer maxPartySize;

    private String tierConfig;

    private String applicableDays;

    @Builder.Default
    private Boolean applyToSpecificItems = false;

    @Builder.Default
    private Boolean active = true;
}
