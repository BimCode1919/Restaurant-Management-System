package com.restaurant.qrorder.domain.dto.response;

import com.restaurant.qrorder.domain.common.PaymentMethod;
import com.restaurant.qrorder.domain.common.PaymentStatus;
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
public class PaymentResponse {

    private Long id;
    private Long billId;
    private PaymentMethod method;
    private BigDecimal amount;
    private PaymentStatus status;
    
    // For online payments
    private String transactionId;
    private String paymentUrl;
    
    // MoMo specific
    private String momoOrderId;
    private String momoRequestId;
    
    private String errorMessage;
    
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
