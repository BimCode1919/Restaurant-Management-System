package com.restaurant.qrorder.domain.dto.request;

import com.restaurant.qrorder.domain.common.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {

    @NotNull(message = "Bill ID is required")
    private Long billId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    // For online payments - return URL after payment
    private String returnUrl;

    // For online payments - cancel URL
    private String cancelUrl;

    // Customer info for MoMo
    private String customerName;
    private String customerPhone;
}
