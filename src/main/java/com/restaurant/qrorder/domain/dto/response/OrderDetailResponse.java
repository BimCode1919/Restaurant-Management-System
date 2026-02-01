package com.restaurant.qrorder.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponse {
    
    private Long id;
    private Long itemId;
    private String itemName;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal subtotal;
    private String notes;
}
