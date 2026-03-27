package com.restaurant.qrorder.domain.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationTImeResponse {
    private Long reservationId;
    private LocalTime startTime;
    private LocalTime endTime;
}
