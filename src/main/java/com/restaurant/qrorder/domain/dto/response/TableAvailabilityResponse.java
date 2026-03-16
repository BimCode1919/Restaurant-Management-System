package com.restaurant.qrorder.domain.dto.response;

import com.restaurant.qrorder.domain.common.TableStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableAvailabilityResponse {
    private Long        id;
    private String      tableNumber;
    private Integer     capacity;
    private String      location;
    private TableStatus status;
    private String      qrCode;
    private boolean     available = true;   // ✅ always true in this response — reserved for clarity
}
