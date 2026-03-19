package com.restaurant.qrorder.domain.dto.response;

import com.restaurant.qrorder.domain.common.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Public-safe view of a booked reservation slot.
 * Does NOT expose any customer PII (name, phone, email).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookedSlotResponse {

    private LocalDateTime reservationTime;
    private LocalDateTime reservationEndTime;   // reservationTime + 2h
    private Integer partySize;
    private List<String> tableNumbers;
    private ReservationStatus status;           // PENDING / CONFIRMED / SEATED
}
