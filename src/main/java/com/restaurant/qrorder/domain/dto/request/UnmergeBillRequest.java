package com.restaurant.qrorder.domain.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UnmergeBillRequest {
    @NotNull(message = "Bill ID must not be null")
    private Long billId;
}