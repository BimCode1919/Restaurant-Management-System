package com.restaurant.qrorder.controller;

import com.restaurant.qrorder.domain.common.ReservationStatus;
import com.restaurant.qrorder.domain.dto.request.CreateReservationRequest;
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

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservation Management", description = "APIs for managing restaurant reservations")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * Create new reservation
     */
    @PostMapping
    @Operation(summary = "Create reservation", description = "Create a new restaurant reservation (Authenticated users)")
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody CreateReservationRequest request,
            Authentication authentication) {
        
        // Get user ID from authentication
        Long userId = getUserIdFromAuth(authentication);
        
        ReservationResponse response = reservationService.createReservation(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get reservation by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get reservation by ID", description = "Retrieve reservation details (Admin/Staff only)")
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable Long id) {
        ReservationResponse response = reservationService.getReservation(id);
        return ResponseEntity.ok(response);
    }

    /**
     * List reservations by date range
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "List reservations", description = "Get reservations by date range or status (Admin/Staff only)")
    public ResponseEntity<List<ReservationResponse>> getReservations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) ReservationStatus status) {
        
        List<ReservationResponse> responses;
        
        if (status != null) {
            responses = reservationService.getReservationsByStatus(status);
        } else if (startDate != null && endDate != null) {
            responses = reservationService.getReservationsByDateRange(startDate, endDate);
        } else {
            // Default: today's reservations
            LocalDateTime start = LocalDateTime.now().withHour(0).withMinute(0);
            LocalDateTime end = start.plusDays(1);
            responses = reservationService.getReservationsByDateRange(start, end);
        }
        
        return ResponseEntity.ok(responses);
    }

    /**
     * Confirm reservation
     */
    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Confirm reservation", description = "Confirm a pending reservation (Admin/Staff only)")
    public ResponseEntity<ReservationResponse> confirmReservation(@PathVariable Long id) {
        ReservationResponse response = reservationService.confirmReservation(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Check-in reservation (customer arrived)
     */
    @PutMapping("/{id}/check-in")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Check-in reservation", description = "Mark customer as arrived and seated (Admin/Staff only)")
    public ResponseEntity<ReservationResponse> checkIn(
            @PathVariable Long id,
            @RequestParam Long billId) {
        
        ReservationResponse response = reservationService.checkIn(id, billId);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel reservation
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Cancel reservation", description = "Cancel a reservation (Admin/Staff only)")
    public ResponseEntity<ReservationResponse> cancelReservation(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        
        ReservationResponse response = reservationService.cancelReservation(id, reason);
        return ResponseEntity.ok(response);
    }

    /**
     * Mark reservation as no-show
     */
    @PutMapping("/{id}/no-show")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Mark as no-show", description = "Mark reservation as no-show (Admin/Staff only)")
    public ResponseEntity<ReservationResponse> markAsNoShow(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        
        ReservationResponse response = reservationService.markAsNoShow(id, reason);
        return ResponseEntity.ok(response);
    }

    // Helper method to extract user ID from authentication
    private Long getUserIdFromAuth(Authentication authentication) {
        // Implement based on your authentication mechanism
        // For now, return a mock value
        return 1L;
    }
}
