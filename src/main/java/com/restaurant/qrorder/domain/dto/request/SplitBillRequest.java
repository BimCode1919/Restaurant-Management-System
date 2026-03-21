package com.restaurant.qrorder.domain.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SplitBillRequest {

    @NotNull(message = "Bill id must not be null")
    private Long billId;

    @NotEmpty(message = "Order detail id must not be empty")
    @Valid
    private List<SplitItem> orderDetailIds;

    @Data
    public static class SplitItem {
        @NotNull
        private Long orderDetailId;

        @NotNull
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;
    }
}