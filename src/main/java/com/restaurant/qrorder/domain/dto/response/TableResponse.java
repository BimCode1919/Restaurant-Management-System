package com.restaurant.qrorder.domain.dto.response;

import com.restaurant.qrorder.domain.common.TableStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableResponse {
    
    private Long id;
    private String tableNumber;
    private BillResponse currentBill;
    private Integer capacity;
    private TableStatus status;
    private String location;
    private String qrCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
