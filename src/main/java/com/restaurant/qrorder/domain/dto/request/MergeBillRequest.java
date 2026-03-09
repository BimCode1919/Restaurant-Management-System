package com.restaurant.qrorder.domain.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class MergeBillRequest {
    @NotNull(message = "Bill IDs must not be null")
    @Size(min = 2, message = "At least two bill IDs are required to merge")
    private List<Long> billIds;
}
