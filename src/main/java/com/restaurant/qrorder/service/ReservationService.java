package com.restaurant.qrorder.service;

import com.restaurant.qrorder.domain.common.BillStatus;
import com.restaurant.qrorder.domain.common.ReservationStatus;
import com.restaurant.qrorder.domain.common.TableStatus;
import com.restaurant.qrorder.domain.dto.request.CreateReservationRequest;
import com.restaurant.qrorder.domain.dto.request.CreateReservationRequestWithoutDeposit;
import com.restaurant.qrorder.domain.dto.response.ReservationResponse;
import com.restaurant.qrorder.domain.entity.*;
import com.restaurant.qrorder.exception.custom.InvalidOperationException;
import com.restaurant.qrorder.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RestaurantTableRepository tableRepository;
    private final UserRepository userRepository;
    private final BillRepository billRepository;

    /**
     * Tạo reservation mới
     */
    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest request, Long userId) {
        // Validate reservation time
        validateReservationTime(request.getReservationTime());

        // Check table availability
        List<RestaurantTable> tables = findAndValidateTables(
                request.getRequestedTableIds(),
                request.getPartySize(),
                request.getReservationTime()
        );

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Create reservation
        Reservation reservation = Reservation.builder()
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .customerEmail(request.getCustomerEmail())
                .partySize(request.getPartySize())
                .reservationTime(request.getReservationTime())
                .status(ReservationStatus.PENDING)
                .note(request.getNote())
                .depositRequired(false)
                .depositAmount(request.getDepositAmount())
                .depositPaid(false)
                .createdBy(user)
                .tables(tables)
                .build();


//        tables.forEach(table -> table.setStatus(TableStatus.RESERVED));
        tableRepository.saveAll(tables);

        Reservation saved = reservationRepository.save(reservation);

        log.info("Created reservation ID: {} for customer: {}", saved.getId(), saved.getCustomerName());

        return mapToResponse(saved);
    }

    @Transactional
    public ReservationResponse createReservationAndBill(CreateReservationRequest request, Long userId) {
        // Validate reservation time
        validateReservationTime(request.getReservationTime());

        // Check table availability
        List<RestaurantTable> tables = findAndValidateTables(
                request.getRequestedTableIds(),
                request.getPartySize(),
                request.getReservationTime()
        );

        boolean isLargeGroup  = request.getPartySize() != null && request.getPartySize() > 10;
        boolean hasPreOrder   = request.getPreOrderItems() != null && !request.getPreOrderItems().isEmpty();

        if (!isLargeGroup && !hasPreOrder) {
            throw new InvalidOperationException(
                    "Deposit reservation requires either party size > 10 or pre-order items. " +
                            "For small groups without pre-order, use standard reservation instead."
            );
        }

        if (request.getDepositAmount() == null || request.getDepositAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Deposit amount is required and must be greater than 0");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));



        // Create reservation
        Reservation reservation = Reservation.builder()
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .customerEmail(request.getCustomerEmail())
                .partySize(request.getPartySize())
                .reservationTime(request.getReservationTime())
                .status(ReservationStatus.PENDING)
                .note(request.getNote())
                .depositRequired(true)
                .depositAmount(request.getDepositAmount())
                .depositPaid(false)
                .createdBy(user)
                .tables(tables)
                .build();



        tables.forEach(table -> table.setStatus(TableStatus.RESERVED));
        Reservation savedReservation = reservationRepository.save(reservation);

        Bill bill = Bill.builder()
                .totalPrice(request.getDepositAmount())
                .partySize(request.getPartySize())
                .discountAmount(BigDecimal.ZERO)
                .finalPrice(BigDecimal.ZERO)
                .status(BillStatus.OPEN)
                .billTables(new ArrayList<>())
                .orders(new ArrayList<>())
                .reservation(savedReservation)
                .build();


        Bill savedBill = billRepository.save(bill);
        savedReservation.setBill(savedBill);
        savedReservation = reservationRepository.save(reservation);


        // Create bill-table associations and update table status
        for (RestaurantTable table : tables) {
            BillTable billTable = BillTable.builder()
                    .id(new BillTable.BillTableId(savedBill.getId(), table.getId()))
                    .bill(savedBill)
                    .table(table)
                    .build();
            table.setStatus(TableStatus.RESERVED);
            savedBill.getBillTables().add(billTable);
        }
        tableRepository.saveAll(tables);
        billRepository.save(savedBill);

        log.info("Created reservation WITH deposit [ID: {}] for customer: {}, partySize: {}, deposit: {}",
                savedReservation.getId(), savedReservation.getCustomerName(),
                request.getPartySize(), request.getDepositAmount());

        return mapToResponse(savedReservation);
    }

    @Transactional
    public ReservationResponse createReservationWithoutDeposit(CreateReservationRequestWithoutDeposit request, Long userId) {
        validateReservationTime(request.getReservationTime());

        // ── Validate: party size must be <= 10 ────────────────────────────────
        if (request.getPartySize() != null && request.getPartySize() > 10) {
            throw new InvalidOperationException(
                    "Large groups (> 10 people) must use deposit reservation. " +
                            "Please use the deposit reservation endpoint instead."
            );
        }

        List<RestaurantTable> tables = findAndValidateTables(
                request.getRequestedTableIds(),
                request.getPartySize(),
                request.getReservationTime()
        );

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // ── Create Reservation only, no Bill needed yet ────────────────────────
        Reservation reservation = Reservation.builder()
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .customerEmail(request.getCustomerEmail())
                .partySize(request.getPartySize())
                .reservationTime(request.getReservationTime())
                .status(ReservationStatus.PENDING)
                .note(request.getNote())
                .depositRequired(false)   // no deposit needed
                .depositAmount(null)
                .depositPaid(false)
                .createdBy(user)
                .tables(tables)
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);

        Bill bill = Bill.builder()
                .totalPrice(BigDecimal.ZERO)
                .partySize(request.getPartySize())
                .discountAmount(BigDecimal.ZERO)
                .finalPrice(BigDecimal.ZERO)
                .status(BillStatus.OPEN)
                .billTables(new ArrayList<>())
                .orders(new ArrayList<>())
                .reservation(savedReservation)
                .build();


        Bill savedBill = billRepository.save(bill);
        savedReservation.setBill(savedBill);
        savedReservation = reservationRepository.save(reservation);


        // Create bill-table associations and update table status
        for (RestaurantTable table : tables) {
            BillTable billTable = BillTable.builder()
                    .id(new BillTable.BillTableId(savedBill.getId(), table.getId()))
                    .bill(savedBill)
                    .table(table)
                    .build();
            table.setStatus(TableStatus.RESERVED);
            savedBill.getBillTables().add(billTable);
        }
        tableRepository.saveAll(tables);
        billRepository.save(savedBill);


        log.info("Created reservation WITHOUT deposit [ID: {}] for customer: {}, partySize: {}",
                savedReservation.getId(), savedReservation.getCustomerName(), request.getPartySize());

        return mapToResponse(savedReservation);
    }

    /**
     * Confirm reservation
     */
    @Transactional
    public ReservationResponse confirmReservation(Long reservationId) {
        Reservation reservation = getReservationById(reservationId);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new RuntimeException("Only pending reservations can be confirmed");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        Reservation saved = reservationRepository.save(reservation);

        log.info("Confirmed reservation ID: {}", reservationId);

        return mapToResponse(saved);
    }

    /**
     * Check-in: Customer arrived and seated
     */
    @Transactional
    public ReservationResponse checkIn(Long reservationId, Long billId) {
        Reservation reservation = getReservationById(reservationId);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new RuntimeException("Only confirmed reservations can be checked in");
        }

        reservation.markAsSeated();

        // Mark tables as occupied
        reservation.getTables().forEach(table -> 
            table.setStatus(TableStatus.OCCUPIED)
        );

        Reservation saved = reservationRepository.save(reservation);

        log.info("Checked in reservation ID: {}, Bill ID: {}", reservationId, billId);

        return mapToResponse(saved);
    }

    /**
     * Cancel reservation
     */
    @Transactional
    public ReservationResponse cancelReservation(Long reservationId, String reason) {
        Reservation reservation = getReservationById(reservationId);

        if (reservation.getStatus() == ReservationStatus.COMPLETED ||
                reservation.getStatus() == ReservationStatus.NO_SHOW) {
            throw new RuntimeException("Cannot cancel completed or no-show reservations");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(LocalDateTime.now());
        reservation.setCancellationReason(reason);

        // Free up tables
        reservation.getTables().forEach(table -> 
            table.setStatus(TableStatus.AVAILABLE)
        );

        Reservation saved = reservationRepository.save(reservation);

        log.info("Cancelled reservation ID: {}, Reason: {}", reservationId, reason);

        return mapToResponse(saved);
    }

    /**
     * Mark as NO_SHOW - scheduled job and manual trigger
     */
    @Transactional
    public ReservationResponse markAsNoShow(Long reservationId, String reason) {
        Reservation reservation = getReservationById(reservationId);

        if (!reservation.isNoShowEligible()) {
            throw new RuntimeException("Reservation is not eligible for no-show");
        }

        reservation.markAsNoShow(reason);

        // Free up tables
        reservation.getTables().forEach(table -> 
            table.setStatus(TableStatus.AVAILABLE)
        );

        Reservation saved = reservationRepository.save(reservation);

        log.warn("Marked reservation ID: {} as NO_SHOW", reservationId);

        return mapToResponse(saved);
    }

    /**
     * Scheduled job: Auto mark no-show reservations
     * Runs every 10 minutes
     */
    @Scheduled(cron = "0 */10 * * * *")
    @Transactional
    public void autoMarkNoShowReservations() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(15);
        List<Reservation> overdueReservations = reservationRepository.findOverdueReservations(cutoffTime);

        for (Reservation reservation : overdueReservations) {
            if (reservation.isNoShowEligible()) {
                markAsNoShow(reservation.getId(), "Auto marked: Customer did not arrive within grace period");
            }
        }

        if (!overdueReservations.isEmpty()) {
            log.info("Auto-marked {} reservations as NO_SHOW", overdueReservations.size());
        }
    }

    /**
     * Get reservation by ID
     */
    @Transactional(readOnly = true)
    public ReservationResponse getReservation(Long reservationId) {
        Reservation reservation = getReservationById(reservationId);
        return mapToResponse(reservation);
    }

    /**
     * List reservations by date range
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByDateRange(LocalDateTime start, LocalDateTime end) {
        List<Reservation> reservations = reservationRepository.findByReservationTimeBetween(start, end);
        return reservations.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * List reservations by status
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByStatus(ReservationStatus status) {
        List<Reservation> reservations = reservationRepository.findByStatus(status);
        return reservations.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Helper methods

    private Reservation getReservationById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found with ID: " + id));
    }

    private void validateReservationTime(LocalDateTime reservationTime) {
        if (reservationTime.isBefore(LocalDateTime.now().plusHours(1))) {
            throw new RuntimeException("Reservation must be at least 1 hour in advance");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime maxReservationDate = now.plusDays(3);

        if (reservationTime.isAfter(maxReservationDate)) {
            throw new IllegalArgumentException("Reservation can only be made within the next 3 days.");
        }

        // Business hours validation (e.g., 9 AM - 10 PM)
        int hour = reservationTime.getHour();
        if (hour < 9 || hour >= 22) {
            throw new RuntimeException("Reservations are only available between 9 AM and 10 PM");
        }
    }

    private List<RestaurantTable> findAndValidateTables(
            List<Long> requestedTableIds,
            Integer partySize,
            LocalDateTime reservationTime) {

        LocalDateTime end = reservationTime.plusHours(2); // Assume 2 hour dining period

        List<RestaurantTable> tables;

        if (requestedTableIds != null && !requestedTableIds.isEmpty()) {
            // Check specific tables
            tables = tableRepository.findAllById(requestedTableIds);
            if (tables.size() != requestedTableIds.size()) {
                throw new RuntimeException("Some requested tables not found");
            }

            // Check if tables are available at that time
            List<Reservation> conflicts = reservationRepository.findConflictingReservations(
                    requestedTableIds, reservationTime, end);

            if (!conflicts.isEmpty()) {
                throw new RuntimeException("Requested tables are not available at the specified time");
            }
        } else {
            // Auto-assign tables based on party size
            tables = tableRepository.findAvailableTablesForTimePeriod(reservationTime, end);

            if (tables.isEmpty()) {
                throw new RuntimeException("No tables available at the specified time");
            }

            // Simple logic: take first available table (can be improved)
            tables = tables.stream().limit(1).collect(Collectors.toList());
        }

        return tables;
    }

    private ReservationResponse mapToResponse(Reservation reservation) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .customerName(reservation.getCustomerName())
                .customerPhone(reservation.getCustomerPhone())
                .customerEmail(reservation.getCustomerEmail())
                .partySize(reservation.getPartySize())
                .reservationTime(reservation.getReservationTime())
                .status(reservation.getStatus())
                .note(reservation.getNote())
                .depositRequired(reservation.getDepositRequired())
                .depositAmount(reservation.getDepositAmount())
                .depositPaid(reservation.getDepositPaid())
                .gracePeriodMinutes(reservation.getGracePeriodMinutes())
                .arrivalTime(reservation.getArrivalTime())
                .cancelledAt(reservation.getCancelledAt())
                .cancellationReason(reservation.getCancellationReason())
                .tableNumbers(reservation.getTables().stream()
                        .map(RestaurantTable::getTableNumber)
                        .collect(Collectors.toList()))
                .billId(reservation.getBill().getId())
                .createdAt(reservation.getCreatedAt())
                .updatedAt(reservation.getUpdatedAt())
                .canCheckIn(reservation.getStatus() == ReservationStatus.CONFIRMED)
                .canCancel(reservation.getStatus() == ReservationStatus.PENDING || 
                          reservation.getStatus() == ReservationStatus.CONFIRMED)
                .canMarkNoShow(reservation.isNoShowEligible())
                .build();
    }
}
