package com.restaurant.qrorder.controller;

import com.restaurant.qrorder.domain.common.ReservationStatus;
import com.restaurant.qrorder.domain.dto.request.CreateReservationRequest;
import com.restaurant.qrorder.domain.dto.request.UpdateReservationRequest;
import com.restaurant.qrorder.domain.dto.response.BookedSlotResponse;
import com.restaurant.qrorder.domain.dto.response.ReservationResponse;
import com.restaurant.qrorder.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservation Management", description = "APIs for managing restaurant reservations")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {

    private final ReservationService reservationService;

    // ─── CREATE ──────────────────────────────────────────────────────────────

    /**
     * Single endpoint for all reservation types.
     * Deposit is auto-determined by the service:
     *   partySize > 10 OR preOrderItems present → deposit required
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER', 'MANAGER', 'CASHIER')")
    @Operation(summary = "Create reservation",
               description = "Deposit auto-determined: partySize > 10 or pre-orders present triggers deposit")
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody CreateReservationRequest request,
            Authentication authentication) {

        ReservationResponse response = reservationService.createReservation(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─── READ ─────────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get reservation by ID")
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.getReservation(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "List reservations by status")
    public ResponseEntity<List<ReservationResponse>> getReservations(
            @RequestParam(required = false) ReservationStatus status) {

        return ResponseEntity.ok(reservationService.getReservationsByStatus(status));
    }

    @GetMapping("/availability")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER', 'MANAGER', 'CASHIER')")
    @Operation(summary = "Get booked slots for a date — safe for customers, no PII exposed",
               description = "Returns PENDING/CONFIRMED/SEATED reservations for the given date. " +
                             "Default: today. Only shows time, tables, party size — no customer info.")
    public ResponseEntity<List<BookedSlotResponse>> getAvailability(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate target = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(reservationService.getBookedSlots(target));
    }

    // ─── UPDATE ───────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Update reservation details (PENDING only)")
    public ResponseEntity<ReservationResponse> updateReservation(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReservationRequest request) {
        return ResponseEntity.ok(reservationService.updateReservation(id, request));
    }

    // ─── STATUS TRANSITIONS ───────────────────────────────────────────────────

    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Confirm a PENDING reservation")
    public ResponseEntity<ReservationResponse> confirmReservation(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.confirmReservation(id));
    }

    @PutMapping("/{id}/check-in")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Check-in: mark customer as arrived and seated")
    public ResponseEntity<ReservationResponse> checkIn(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.checkIn(id));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER')")
    @Operation(summary = "Cancel a reservation")
    public ResponseEntity<ReservationResponse> cancelReservation(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(reservationService.cancelReservation(id, reason));
    }

    @PutMapping("/{id}/no-show")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER')")
    @Operation(summary = "Mark reservation as no-show")
    public ResponseEntity<ReservationResponse> markAsNoShow(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(reservationService.markAsNoShow(id, reason));
    }

    // ─── DEPOSIT ──────────────────────────────────────────────────────────────

    @PutMapping("/{id}/deposit-paid")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    @Operation(summary = "Mark reservation deposit as paid")
    public ResponseEntity<ReservationResponse> markDepositPaid(@PathVariable Long id) {
        return ResponseEntity.ok(reservationService.markDepositAsPaid(id));
    }

    // ─── ADMIN TRIGGER ────────────────────────────────────────────────────────

    @PostMapping("/reserve-tables")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Manually trigger the auto-reserve-tables scheduled job")
    public ResponseEntity<String> triggerReserveTables() {
        reservationService.autoReserveTablesBeforeReservation();
        return ResponseEntity.ok("Auto-reserve tables job triggered successfully");
    }
}
