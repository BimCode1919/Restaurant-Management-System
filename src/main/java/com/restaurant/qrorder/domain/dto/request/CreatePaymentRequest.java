package com.restaurant.qrorder.domain.dto.request;

import com.restaurant.qrorder.domain.common.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {

    @NotNull(message = "Bill ID is required")
    private Long billId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    // Optional: For online payments - return URL after payment
    // If not provided, will use default URL
    private String returnUrl;
}
