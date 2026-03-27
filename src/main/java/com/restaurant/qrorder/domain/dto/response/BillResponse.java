package com.restaurant.qrorder.domain.dto.response;

import com.restaurant.qrorder.domain.common.BillStatus;
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
public class BillResponse {
    
    private Long id;
    private BigDecimal totalPrice;
    private Integer partySize;
    private DiscountResponse discount;
    private BigDecimal discountAmount;
    private BigDecimal finalPrice;
    private BillStatus status;
    private Long reservationId;
    private Long paymentId;
    private List<String> tableNumbers;
    private List<OrderResponse> orders;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;
    private ReservationTImeResponse reservationResponses;

}
