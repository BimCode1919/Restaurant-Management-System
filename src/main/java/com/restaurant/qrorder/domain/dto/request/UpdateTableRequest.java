package com.restaurant.qrorder.domain.dto.request;

import com.restaurant.qrorder.domain.common.TableStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTableRequest {
    
    private String tableNumber;
    
    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 20, message = "Capacity cannot exceed 20")
    private Integer capacity;
    
    private String location;
    
    private TableStatus status;
}
