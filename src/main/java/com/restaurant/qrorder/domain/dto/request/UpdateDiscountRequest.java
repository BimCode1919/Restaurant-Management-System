package com.restaurant.qrorder.domain.dto.request;

import com.restaurant.qrorder.domain.common.DiscountType;
import com.restaurant.qrorder.domain.common.DiscountValueType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
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
public class UpdateDiscountRequest {

    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private DiscountType discountType;

    private DiscountValueType valueType;

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

    // For PARTY_SIZE discount
    @Min(value = 1, message = "Min party size must be at least 1")
    private Integer minPartySize;

    private Integer maxPartySize;

    // For BILL_TIER discount
    private String tierConfig;

    // For HOLIDAY discount
    private String applicableDays;

    // For ITEM_SPECIFIC discount
    private Boolean applyToSpecificItems;

    private Boolean active;
}
