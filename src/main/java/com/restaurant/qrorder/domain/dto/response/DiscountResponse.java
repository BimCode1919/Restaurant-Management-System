package com.restaurant.qrorder.domain.dto.response;

import com.restaurant.qrorder.domain.common.DiscountType;
import com.restaurant.qrorder.domain.common.DiscountValueType;
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
public class DiscountResponse {

    private Long id;
    private String name;
    private String description;
    private DiscountType discountType;
    private DiscountValueType valueType;
    private BigDecimal value;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer usageLimit;
    private Integer usedCount;
    
    private Integer minPartySize;
    private Integer maxPartySize;
    private String tierConfig;
    private String applicableDays;
    private Boolean applyToSpecificItems;
    
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
