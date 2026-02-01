package com.restaurant.qrorder.domain.dto.response;

import com.restaurant.qrorder.domain.common.OrderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    
    private Long id;
    private Long billId;
    private OrderType orderType;
    private String createdBy;
    private BigDecimal totalAmount;
    private List<OrderDetailResponse> items;
    private LocalDateTime createdAt;
}
