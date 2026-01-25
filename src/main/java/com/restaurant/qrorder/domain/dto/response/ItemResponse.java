package com.restaurant.qrorder.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
public class ItemResponse {

    Long id;
    String name;
    BigDecimal price;
    String unit;
    String categoryName;
    String description;
    String imageUrl;
    Boolean available;
    
    List<ActiveDiscountInfo> activeDiscounts;
    
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveDiscountInfo {
        Long discountId;
        String discountName;
        BigDecimal discountValue;
        String discountType;
    }
}
