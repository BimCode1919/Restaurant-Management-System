package com.restaurant.qrorder.domain.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBillRequest {
    
    @NotEmpty(message = "At least one table is required")
    private List<Long> tableIds;
    
    @NotNull(message = "Party size is required")
    @Min(value = 1, message = "Party size must be at least 1")
    private Integer partySize;
    
    private Long reservationId;
}
