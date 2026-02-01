package com.restaurant.qrorder.domain.dto.request;

import com.restaurant.qrorder.domain.common.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReservationRequest {

    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private Integer partySize;
    private LocalDateTime reservationTime;
    private ReservationStatus status;
    private String note;
    private List<Long> tableIds;
    private String cancellationReason;
}
