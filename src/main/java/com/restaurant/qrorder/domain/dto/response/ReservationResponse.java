package com.restaurant.qrorder.domain.dto.response;

import com.restaurant.qrorder.domain.common.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {

    private Long id;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private Integer partySize;
    private LocalDateTime reservationTime;
    private ReservationStatus status;
    private String note;
    
    private Boolean depositRequired;
    private BigDecimal depositAmount;
    private Boolean depositPaid;
    
    private Integer gracePeriodMinutes;
    private LocalDateTime arrivalTime;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    
    private List<String> tableNumbers;
    private Long billId;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private Boolean canCheckIn;
    private Boolean canCancel;
    private Boolean canMarkNoShow;
}
