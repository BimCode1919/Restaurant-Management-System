package com.restaurant.qrorder.service;

import com.restaurant.qrorder.domain.common.*;
import com.restaurant.qrorder.domain.dto.request.CreateReservationRequest;
import com.restaurant.qrorder.domain.dto.request.UpdateReservationRequest;
import com.restaurant.qrorder.domain.dto.response.BookedSlotResponse;
import com.restaurant.qrorder.domain.dto.response.ReservationResponse;
import com.restaurant.qrorder.domain.entity.*;
import com.restaurant.qrorder.exception.custom.InvalidOperationException;
import com.restaurant.qrorder.exception.custom.ResourceNotFoundException;
import com.restaurant.qrorder.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    // ─── Constants ────────────────────────────────────────────────────────────
    private static final BigDecimal TABLE_FEE        = BigDecimal.valueOf(300_000L);
    private static final BigDecimal DEPOSIT_RATE     = new BigDecimal("0.10");
    private static final int        DINING_HOURS     = 2;
    private static final int        LARGE_GROUP_SIZE = 10;
    private static final Long        DEFAULT_ACCOUNT_ID = 8L;
    // ─── Dependencies ─────────────────────────────────────────────────────────
    private final ReservationRepository     reservationRepository;
    private final RestaurantTableRepository tableRepository;
    private final UserRepository            userRepository;
    private final BillRepository            billRepository;
    private final ItemRepository            itemRepository;
    private final OrderRepository           orderRepository;
    private final ReservationMailService    reservationMailService;

    // Self-injection via proxy — required for @Transactional isolation in scheduler
    // (calling this.method() bypasses Spring proxy; self.method() does not)
    @Lazy
    @Autowired
    private ReservationService self;

    // ─── Internal record ──────────────────────────────────────────────────────
    private record ResolvedPreOrderItem(Item item, int quantity) {}

    // ═════════════════════════════════════════════════════════════════════════
    // PUBLIC — CREATE
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Unified reservation creation.
     * Deposit is auto-determined:
     *   - partySize > LARGE_GROUP_SIZE  → requires deposit
     *   - preOrderItems present         → requires deposit
     * Always creates a Bill (OPEN) linked to the reservation.
     */
    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest request,
                                                  String creatorEmail) {
        // 1. Validate reservation time
        validateReservationTime(request.getReservationTime());

        // 2. Validate and fetch tables (excludeId=0L → no reservation to exclude)
        List<RestaurantTable> tables = findAndValidateTables(
                request.getRequestedTableIds(),
                request.getPartySize(),
                request.getReservationTime(),
                0L);

        // 3. Resolve the authenticated user (no more hardcoded userId=1)
        User user = resolveUserOptional(creatorEmail);

        if (user == null) {
            // ✅ findById actually hits DB and verifies the user exists
            user = userRepository.findById(DEFAULT_ACCOUNT_ID)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Default guest account not found. Please ensure user ID "
                                    + DEFAULT_ACCOUNT_ID + " exists in the system."));
        }

        // 4. Resolve pre-order items and compute subtotal
        List<ResolvedPreOrderItem> resolvedItems =
                resolvePreOrderItems(request.getPreOrderItems());

        BigDecimal preOrderTotal = resolvedItems.stream()
                .map(ri -> ri.item().getPrice().multiply(BigDecimal.valueOf(ri.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 5. Determine deposit requirement and amount
        boolean isLargeGroup    = request.getPartySize() > LARGE_GROUP_SIZE;
        boolean hasPreOrders    = !resolvedItems.isEmpty();
        boolean requiresDeposit = isLargeGroup || hasPreOrders;

        BigDecimal depositAmount = requiresDeposit
                ? calculateDeposit(preOrderTotal, tables.size())
                : null;



        // 6. Persist Reservation
        Reservation reservation = Reservation.builder()
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .customerEmail(request.getCustomerEmail())
                .partySize(request.getPartySize())
                .reservationTime(request.getReservationTime())
                .status(ReservationStatus.PENDING)
                .note(request.getNote())
                .depositRequired(requiresDeposit)
                .depositAmount(depositAmount)
                .depositPaid(false)
                .createdBy(user)
                .tables(tables)
                .build();

        Reservation saved = reservationRepository.save(reservation);

        // 7. Bill initial total = preOrderTotal − deposit (remaining ≥ 0)
        BigDecimal billTotal = requiresDeposit
                ? preOrderTotal.subtract(depositAmount).max(BigDecimal.ZERO)
                : BigDecimal.ZERO;

        // 8. Create Bill
        Bill savedBill = createBill(saved, request.getPartySize(), billTotal);

        // 9. Link Bill ↔ Reservation (save on the same `saved` reference)
        saved.setBill(savedBill);
        saved = reservationRepository.save(saved);

        // 10. Create BillTable join records
        linkBillTables(savedBill, tables);

        // 11. Create pre-order Order + OrderDetails if present
        if (!resolvedItems.isEmpty()) {
            createPreOrders(savedBill, resolvedItems, user, saved);
        }

        // 12. Table status is managed by the scheduled job (autoReserveTablesBeforeReservation)
        //     which sets RESERVED 2 hours before the reservation time.
        //     Do NOT set RESERVED immediately here — it would block other time slots.

        log.info("Created reservation [ID:{}] customer:{} requiresDeposit:{} depositAmount:{}",
                saved.getId(), saved.getCustomerName(), requiresDeposit, depositAmount);

        return mapToResponse(saved);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PUBLIC — STATUS TRANSITIONS
    // ═════════════════════════════════════════════════════════════════════════

    @Transactional
    public ReservationResponse confirmReservation(Long id) {
        Reservation reservation = getReservationById(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new InvalidOperationException(
                    "Only PENDING reservations can be confirmed. Current status: "
                    + reservation.getStatus());
        }

        // FIX: cannot confirm if deposit required but not yet paid
        if (Boolean.TRUE.equals(reservation.getDepositRequired())
                && !Boolean.TRUE.equals(reservation.getDepositPaid())) {
            throw new InvalidOperationException(
                    "Cannot confirm reservation [ID:" + reservation.getId()
                    + "]: deposit of " + reservation.getDepositAmount() + " VND has not been paid");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        Reservation saved = reservationRepository.save(reservation);

        log.info("Confirmed reservation [ID:{}]", id);

        ReservationResponse response = mapToResponse(saved);
        reservationMailService.sendReservationConfirmedMail(response);
        return response;
    }

    @Transactional
    public ReservationResponse checkIn(Long id) {
        Reservation reservation = getReservationById(id);

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new InvalidOperationException(
                    "Only CONFIRMED reservations can be checked in. Current status: "
                    + reservation.getStatus());
        }

        reservation.markAsSeated();

        // FIX: must saveAll tables explicitly — @ManyToMany has no cascade from Reservation
        reservation.getTables().forEach(t -> t.setStatus(TableStatus.OCCUPIED));
        tableRepository.saveAll(reservation.getTables());

        Reservation saved = reservationRepository.save(reservation);

        // FIX: log now supplies both arguments
        Long billId = saved.getBill() != null ? saved.getBill().getId() : null;
        log.info("Checked in reservation [ID:{}] bill [ID:{}]", id, billId);

        return mapToResponse(saved);
    }

    @Transactional
    public ReservationResponse cancelReservation(Long id, String reason) {
        Reservation reservation = getReservationById(id);

        // FIX: also block SEATED — customer is already dining, bill is active
        if (reservation.getStatus() == ReservationStatus.COMPLETED
                || reservation.getStatus() == ReservationStatus.NO_SHOW
                || reservation.getStatus() == ReservationStatus.SEATED) {
            throw new InvalidOperationException(
                    "Cannot cancel reservations with status: " + reservation.getStatus());
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(LocalDateTime.now());
        reservation.setCancellationReason(reason);

        // FIX: free tables via saveAll (no cascade)
        freeUpTables(reservation.getTables());

        // FIX: close the associated bill
        if (reservation.getBill() != null) {
            reservation.getBill().setStatus(BillStatus.CANCELLED);
            billRepository.save(reservation.getBill());
        }

        Reservation saved = reservationRepository.save(reservation);

        log.info("Cancelled reservation [ID:{}] reason:{}", id, reason);

        ReservationResponse response = mapToResponse(saved);
        reservationMailService.sendReservationCancelledMail(response);
        return response;
    }

    @Transactional
    public ReservationResponse markAsNoShow(Long id, String reason) {
        Reservation reservation = getReservationById(id);

        if (!reservation.isNoShowEligible()) {
            throw new InvalidOperationException(
                    "Reservation [ID:" + id + "] is not eligible for no-show. "
                    + "Status must be CONFIRMED and past grace period.");
        }

        reservation.markAsNoShow(reason);

        // FIX: free tables via saveAll
        freeUpTables(reservation.getTables());

        Reservation saved = reservationRepository.save(reservation);

        log.warn("Marked reservation [ID:{}] as NO_SHOW", id);

        ReservationResponse response = mapToResponse(saved);
        reservationMailService.sendReservationNoShowMail(response);
        return response;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PUBLIC — DEPOSIT
    // ═════════════════════════════════════════════════════════════════════════

    @Transactional
    public ReservationResponse markDepositAsPaid(Long id) {
        Reservation reservation = getReservationById(id);

        if (!Boolean.TRUE.equals(reservation.getDepositRequired())) {
            throw new InvalidOperationException("This reservation does not require a deposit");
        }
        if (Boolean.TRUE.equals(reservation.getDepositPaid())) {
            throw new InvalidOperationException("Deposit is already marked as paid");
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED
                || reservation.getStatus() == ReservationStatus.NO_SHOW) {
            throw new InvalidOperationException(
                    "Cannot mark deposit paid for a CANCELLED or NO_SHOW reservation");
        }

        reservation.setDepositPaid(true);
        Reservation saved = reservationRepository.save(reservation);

        log.info("Deposit paid for reservation [ID:{}] amount:{}", id, saved.getDepositAmount());
        return mapToResponse(saved);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PUBLIC — UPDATE
    // ═════════════════════════════════════════════════════════════════════════

    @Transactional
    public ReservationResponse updateReservation(Long id, UpdateReservationRequest request) {
        Reservation reservation = getReservationById(id);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new InvalidOperationException(
                    "Only PENDING reservations can be updated. Current status: "
                    + reservation.getStatus());
        }

        if (request.getCustomerName()  != null) reservation.setCustomerName(request.getCustomerName());
        if (request.getCustomerPhone() != null) reservation.setCustomerPhone(request.getCustomerPhone());
        if (request.getCustomerEmail() != null) reservation.setCustomerEmail(request.getCustomerEmail());
        if (request.getPartySize()     != null) reservation.setPartySize(request.getPartySize());
        if (request.getNote()          != null) reservation.setNote(request.getNote());

        if (request.getReservationTime() != null) {
            validateReservationTime(request.getReservationTime());
            // FIX: re-check table conflicts at the new time, excluding this reservation itself
            List<Long> currentTableIds = reservation.getTables().stream()
                    .map(RestaurantTable::getId).toList();
            findAndValidateTables(
                    currentTableIds,
                    reservation.getPartySize(),
                    request.getReservationTime(),
                    reservation.getId());
            reservation.setReservationTime(request.getReservationTime());
        }

        return mapToResponse(reservationRepository.save(reservation));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PUBLIC — QUERY
    // ═════════════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ReservationResponse getReservation(Long id) {
        return mapToResponse(getReservationById(id));
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByStatus(ReservationStatus status) {
        if (status == null) {
            // default: today's reservations
            LocalDateTime start = LocalDate.now().atStartOfDay();
            LocalDateTime end   = start.plusDays(1);
            return reservationRepository.findByReservationTimeBetween(start, end)
                    .stream().map(this::mapToResponse).toList();
        }
        return reservationRepository.findByStatus(status)
                .stream().map(this::mapToResponse).toList();
    }

    /**
     * Returns booked slots for a given date — no customer PII exposed.
     * Only PENDING, CONFIRMED, SEATED reservations are included
     * (CANCELLED / NO_SHOW are invisible to the public).
     */
    @Transactional(readOnly = true)
    public List<BookedSlotResponse> getBookedSlots(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end   = date.plusDays(1).atStartOfDay();

        return reservationRepository.findByReservationTimeBetween(start, end)
                .stream()
                .filter(r -> r.getStatus() == ReservationStatus.PENDING
                          || r.getStatus() == ReservationStatus.CONFIRMED
                          || r.getStatus() == ReservationStatus.SEATED)
                .map(r -> BookedSlotResponse.builder()
                        .reservationTime(r.getReservationTime())
                        .reservationEndTime(r.getReservationTime().plusHours(DINING_HOURS))
                        .partySize(r.getPartySize())
                        .tableNumbers(r.getTables().stream()
                                .map(t -> t.getTableNumber()).toList())
                        .status(r.getStatus())
                        .build())
                .toList();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // SCHEDULERS
    // ═════════════════════════════════════════════════════════════════════════

    // FIX: NOT @Transactional here — each self.markAsNoShow() runs in its own transaction
    // via Spring proxy. If one fails, only that reservation rolls back, not the whole batch.
    @Scheduled(cron = "0 */10 * * * *")
    public void autoMarkNoShowReservations() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);
        List<Reservation> overdue = reservationRepository.findOverdueReservations(cutoff);

        int count = 0;
        for (Reservation r : overdue) {
            if (r.isNoShowEligible()) {
                try {
                    self.markAsNoShow(r.getId(), "Auto: customer did not arrive within grace period");
                    count++;
                } catch (Exception e) {
                    log.error("Failed to auto-mark reservation [ID:{}] as NO_SHOW: {}",
                            r.getId(), e.getMessage());
                }
            }
        }
        if (count > 0) {
            log.info("Auto-marked {} reservation(s) as NO_SHOW", count);
        }
    }

    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void autoReserveTablesBeforeReservation() {
        LocalDateTime now        = LocalDateTime.now();
        LocalDateTime twoHrsLater = now.plusHours(DINING_HOURS);

        List<Reservation> upcoming =
                reservationRepository.findReservationsToReserveTable(now, twoHrsLater);

        if (upcoming.isEmpty()) return;

        for (Reservation r : upcoming) {
            List<RestaurantTable> toReserve = r.getTables().stream()
                    .filter(t -> t.getStatus() == TableStatus.AVAILABLE)
                    .toList();

            if (!toReserve.isEmpty()) {
                toReserve.forEach(t -> t.setStatus(TableStatus.RESERVED));
                tableRepository.saveAll(toReserve);
                log.info("Auto-reserved {} table(s) for reservation [ID:{}] at {}",
                        toReserve.size(), r.getId(), r.getReservationTime());
            }
        }

        log.info("Auto-reserve job processed {} upcoming reservation(s)", upcoming.size());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    private User resolveUserOptional(String email) {
        if (email == null || email.isBlank() || email.equals("anonymousUser")) {
            return null; // guest booking — no user required
        }
        return userRepository.findByEmail(email).orElse(null); // soft fail for guests
    }
    private Reservation getReservationById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
    }

    private void validateReservationTime(LocalDateTime time) {
        LocalDateTime now = LocalDateTime.now();

        if (time.isBefore(now.plusHours(1))) {
            throw new InvalidOperationException(
                    "Reservation must be at least 1 hour in advance");
        }
        if (time.isAfter(now.plusDays(3))) {
            throw new InvalidOperationException(
                    "Reservation can only be made within the next 3 days");
        }

        int hour = time.getHour();
        if (hour < 9 || hour >= 22) {
            throw new InvalidOperationException(
                    "Reservations are only available between 9 AM and 10 PM");
        }
    }

    // excludeReservationId: pass 0L when creating, pass reservation.getId() when updating
    private List<RestaurantTable> findAndValidateTables(List<Long> tableIds,
                                                         Integer partySize,
                                                         LocalDateTime startTime,
                                                         Long excludeReservationId) {
        LocalDateTime endTime       = startTime.plusHours(DINING_HOURS);
        // FIX: correct overlap: existing.start < newEnd AND existing.start > newStart - DINING_HOURS
        LocalDateTime startMinus2h  = startTime.minusHours(DINING_HOURS);

        if (tableIds != null && !tableIds.isEmpty()) {
            List<RestaurantTable> tables = tableRepository.findAllById(tableIds);
            if (tables.size() != tableIds.size()) {
                throw new ResourceNotFoundException("One or more requested tables not found");
            }

            // FIX: pass correct overlap params + excludeId; now also checks PENDING reservations
            List<Reservation> conflicts = reservationRepository
                    .findConflictingReservations(tableIds, endTime, startMinus2h, excludeReservationId);
            if (!conflicts.isEmpty()) {
                throw new InvalidOperationException(
                        "One or more requested tables are not available at the specified time");
            }
            return tables;
        }

        // Auto-assign: find available tables in the time window
        List<RestaurantTable> available =
                tableRepository.findAvailableTablesForTimePeriod(startTime, endTime, startMinus2h);
        if (available.isEmpty()) {
            throw new InvalidOperationException(
                    "No tables available at the requested time");
        }
        return new ArrayList<>(available.stream().limit(1).toList());
    }

    private List<ResolvedPreOrderItem> resolvePreOrderItems(
            List<CreateReservationRequest.PreOrderItemRequest> preOrderItems) {

        if (preOrderItems == null || preOrderItems.isEmpty()) return List.of();

        List<ResolvedPreOrderItem> result = new ArrayList<>();
        for (var pi : preOrderItems) {
            Item item = itemRepository.findById(pi.getItemId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Item not found: " + pi.getItemId()));
            if (!item.getAvailable()) {
                throw new InvalidOperationException(
                        "Item '" + item.getName() + "' is not available");
            }
            result.add(new ResolvedPreOrderItem(item, pi.getQuantity()));
        }
        return result;
    }

    private BigDecimal calculateDeposit(BigDecimal preOrderTotal, int tableCount) {
        BigDecimal tableFee = TABLE_FEE.multiply(BigDecimal.valueOf(tableCount));
        return preOrderTotal.add(tableFee)
                .multiply(DEPOSIT_RATE)
                .setScale(0, RoundingMode.HALF_UP);
    }

    private Bill createBill(Reservation reservation, Integer partySize, BigDecimal totalPrice) {
        Bill bill = Bill.builder()
                .totalPrice(totalPrice)
                .finalPrice(totalPrice)
                .partySize(partySize)
                .discountAmount(BigDecimal.ZERO)
                .status(BillStatus.OPEN)
                .billTables(new ArrayList<>())
                .orders(new ArrayList<>())
                .reservation(reservation)
                .build();
        return billRepository.save(bill);
    }

    private void linkBillTables(Bill bill, List<RestaurantTable> tables) {
        for (RestaurantTable table : tables) {
            BillTable bt = BillTable.builder()
                    .id(new BillTable.BillTableId(bill.getId(), table.getId()))
                    .bill(bill)
                    .table(table)
                    .build();
            bill.getBillTables().add(bt);
        }
        billRepository.save(bill);
    }

    private void createPreOrders(Bill bill, List<ResolvedPreOrderItem> items,
                                  User user, Reservation reservation) {
        // FIX: set reservation on Order so that Reservation.preOrders (@OneToMany mappedBy="reservation")
        // is correctly populated and cascade operations work
        Order preOrder = Order.builder()
                .bill(bill)
                .reservation(reservation)
                .orderType(OrderType.PRE_ORDER)
                .createdBy(user)
                .orderDetails(new ArrayList<>())
                .build();

        Order savedOrder = orderRepository.save(preOrder);

        for (ResolvedPreOrderItem ri : items) {
            OrderDetail detail = OrderDetail.builder()
                    .order(savedOrder)
                    .item(ri.item())
                    .quantity(ri.quantity())
                    .price(ri.item().getPrice())
                    .itemStatus(ItemStatus.PENDING)
                    .note("Pre-order for reservation #" + reservation.getId())
                    .build();
            savedOrder.getOrderDetails().add(detail);
        }

        orderRepository.save(savedOrder);
        bill.getOrders().add(savedOrder);
        billRepository.save(bill);

        log.info("Pre-order [ID:{}] created with {} item(s) for reservation [ID:{}]",
                savedOrder.getId(), items.size(), reservation.getId());
    }

    private void freeUpTables(List<RestaurantTable> tables) {
        // FIX: only free tables that are RESERVED (not OCCUPIED — another group may be seated)
        List<RestaurantTable> toFree = tables.stream()
                .filter(t -> t.getStatus() == TableStatus.RESERVED)
                .toList();
        toFree.forEach(t -> t.setStatus(TableStatus.AVAILABLE));
        if (!toFree.isEmpty()) {
            tableRepository.saveAll(toFree);
        }
    }

    private ReservationResponse mapToResponse(Reservation r) {
        return ReservationResponse.builder()
                .id(r.getId())
                .customerName(r.getCustomerName())
                .customerPhone(r.getCustomerPhone())
                .customerEmail(r.getCustomerEmail())
                .partySize(r.getPartySize())
                .reservationTime(r.getReservationTime())
                .status(r.getStatus())
                .note(r.getNote())
                .depositRequired(r.getDepositRequired())
                .depositAmount(r.getDepositAmount())
                .depositPaid(r.getDepositPaid())
                .gracePeriodMinutes(r.getGracePeriodMinutes())
                .arrivalTime(r.getArrivalTime())
                .cancelledAt(r.getCancelledAt())
                .cancellationReason(r.getCancellationReason())
                .tableNumbers(r.getTables().stream()
                        .map(RestaurantTable::getTableNumber).toList())
                // FIX: null-safe — bill may not exist yet
                .billId(r.getBill() != null ? r.getBill().getId() : null)
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .canCheckIn(r.getStatus() == ReservationStatus.CONFIRMED)
                .canCancel(r.getStatus() == ReservationStatus.PENDING
                        || r.getStatus() == ReservationStatus.CONFIRMED)
                .canMarkNoShow(r.isNoShowEligible())
                .build();
    }
}
