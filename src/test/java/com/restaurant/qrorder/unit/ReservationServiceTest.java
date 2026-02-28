package com.restaurant.qrorder.unit;

import com.restaurant.qrorder.domain.common.ReservationStatus;
import com.restaurant.qrorder.domain.common.TableStatus;
import com.restaurant.qrorder.domain.dto.request.CreateReservationRequest;
import com.restaurant.qrorder.domain.dto.response.ReservationResponse;
import com.restaurant.qrorder.domain.entity.*;
import com.restaurant.qrorder.repository.ReservationRepository;
import com.restaurant.qrorder.repository.RestaurantTableRepository;
import com.restaurant.qrorder.repository.UserRepository;
import com.restaurant.qrorder.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationService Unit Tests")
class ReservationServiceTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private RestaurantTableRepository tableRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private ReservationService reservationService;

    private RestaurantTable table;
    private User user;
    private Reservation pendingReservation;
    private Reservation confirmedReservation;
    private Reservation cancelledReservation;
    private Reservation completedReservation;
    private Reservation noShowReservation;

    private LocalDateTime validReservationTime;

    @BeforeEach
    void setUp() {
        validReservationTime = LocalDateTime.now().plusHours(2).withHour(14);

        table = RestaurantTable.builder()
                .id(1L)
                .tableNumber("T01")
                .status(TableStatus.AVAILABLE)
                .capacity(4)
                .build();

        user = new User();
        user.setId(1L);
        user.setFullName("Staff Member");

        pendingReservation = buildReservation(1L, ReservationStatus.PENDING);
        confirmedReservation = buildReservation(2L, ReservationStatus.CONFIRMED);
        cancelledReservation = buildReservation(3L, ReservationStatus.CANCELLED);
        completedReservation = buildReservation(4L, ReservationStatus.COMPLETED);
        noShowReservation = buildReservation(5L, ReservationStatus.NO_SHOW);
    }

    private Reservation buildReservation(Long id, ReservationStatus status) {
        Reservation r = new Reservation();
        r.setId(id);
        r.setStatus(status);
        r.setCustomerName("John Doe");
        r.setCustomerPhone("0123456789");
        r.setCustomerEmail("john@example.com");
        r.setPartySize(2);
        r.setReservationTime(validReservationTime);
        r.setDepositRequired(false);
        r.setDepositPaid(false);
        r.setTables(new ArrayList<>(List.of(table)));
        return r;
    }

    @Nested
    @DisplayName("createReservation()")
    class CreateReservation {

        private CreateReservationRequest request;

        @BeforeEach
        void setUp() {
            request = new CreateReservationRequest();
            request.setCustomerName("John Doe");
            request.setCustomerPhone("0123456789");
            request.setPartySize(2);
            request.setReservationTime(validReservationTime);
            request.setRequestedTableIds(List.of(1L));
            request.setDepositRequired(null);
        }

        @Test
        @DisplayName("reservationTime less than 1 hour ahead → throws")
        void tooSoon_throws() {
            request.setReservationTime(LocalDateTime.now().plusMinutes(30));

            assertThatThrownBy(() -> reservationService.createReservation(request, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("1 hour in advance");
        }

        @Test
        @DisplayName("reservationTime before 9 AM → throws")
        void beforeBusinessHours_throws() {
            request.setReservationTime(LocalDateTime.now().plusDays(1).withHour(8).withMinute(0));

            assertThatThrownBy(() -> reservationService.createReservation(request, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("9 AM and 10 PM");
        }

        @Test
        @DisplayName("reservationTime at or after 10 PM → throws")
        void afterBusinessHours_throws() {
            request.setReservationTime(LocalDateTime.now().plusDays(1).withHour(22).withMinute(0));

            assertThatThrownBy(() -> reservationService.createReservation(request, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("9 AM and 10 PM");
        }

        @Test
        @DisplayName("some requested tables not found → throws")
        void someTablesNotFound_throws() {
            when(tableRepository.findAllById(List.of(1L))).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> reservationService.createReservation(request, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Some requested tables not found");
        }

        @Test
        @DisplayName("requested tables have conflicting reservation → throws")
        void conflictingReservation_throws() {
            when(tableRepository.findAllById(List.of(1L))).thenReturn(List.of(table));
            when(reservationRepository.findConflictingReservations(any(), any(), any()))
                    .thenReturn(List.of(pendingReservation));

            assertThatThrownBy(() -> reservationService.createReservation(request, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not available at the specified time");
        }

        @Test
        @DisplayName("no requested tables, auto-assign, no tables available → throws")
        void autoAssign_noTablesAvailable_throws() {
            request.setRequestedTableIds(null);
            when(tableRepository.findAvailableTablesForTimePeriod(any(), any()))
                    .thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> reservationService.createReservation(request, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("No tables available");
        }

        @Test
        @DisplayName("no requested tables, auto-assign, tables available → assigns first table")
        void autoAssign_tablesAvailable_assignsFirst() {
            request.setRequestedTableIds(null);
            when(tableRepository.findAvailableTablesForTimePeriod(any(), any()))
                    .thenReturn(List.of(table, new RestaurantTable()));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(reservationRepository.save(any(Reservation.class))).thenReturn(pendingReservation);

            ReservationResponse result = reservationService.createReservation(request, 1L);

            assertThat(result).isNotNull();
            verify(tableRepository).saveAll(argThat(tables -> ((List<?>) tables).size() == 1));
        }

        @Test
        @DisplayName("empty requestedTableIds list → auto-assign path taken")
        void emptyTableIds_autoAssignPath() {
            request.setRequestedTableIds(Collections.emptyList());
            when(tableRepository.findAvailableTablesForTimePeriod(any(), any()))
                    .thenReturn(List.of(table));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(reservationRepository.save(any(Reservation.class))).thenReturn(pendingReservation);

            ReservationResponse result = reservationService.createReservation(request, 1L);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("user NOT found → throws RuntimeException")
        void userNotFound_throws() {
            when(tableRepository.findAllById(List.of(1L))).thenReturn(List.of(table));
            when(reservationRepository.findConflictingReservations(any(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.createReservation(request, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("depositRequired null → defaults to false")
        void nullDepositRequired_defaultsFalse() {
            request.setDepositRequired(null);
            when(tableRepository.findAllById(List.of(1L))).thenReturn(List.of(table));
            when(reservationRepository.findConflictingReservations(any(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(reservationRepository.save(any(Reservation.class))).thenReturn(pendingReservation);

            reservationService.createReservation(request, 1L);

            verify(reservationRepository).save(argThat(r -> !r.getDepositRequired()));
        }

        @Test
        @DisplayName("depositRequired true → set to true")
        void depositRequiredTrue_setsTrue() {
            request.setDepositRequired(true);
            when(tableRepository.findAllById(List.of(1L))).thenReturn(List.of(table));
            when(reservationRepository.findConflictingReservations(any(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(reservationRepository.save(any(Reservation.class))).thenReturn(pendingReservation);

            reservationService.createReservation(request, 1L);

            verify(reservationRepository).save(argThat(Reservation::getDepositRequired));
        }

        @Test
        @DisplayName("valid request → tables marked RESERVED and reservation saved")
        void valid_tablesReservedAndSaved() {
            when(tableRepository.findAllById(List.of(1L))).thenReturn(List.of(table));
            when(reservationRepository.findConflictingReservations(any(), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(reservationRepository.save(any(Reservation.class))).thenReturn(pendingReservation);

            reservationService.createReservation(request, 1L);

            assertThat(table.getStatus()).isEqualTo(TableStatus.RESERVED);
            verify(tableRepository).saveAll(any());
            verify(reservationRepository).save(any(Reservation.class));
        }
    }

    @Nested
    @DisplayName("confirmReservation()")
    class ConfirmReservation {

        @Test
        @DisplayName("reservation NOT found → throws")
        void notFound_throws() {
            when(reservationRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.confirmReservation(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Reservation not found");
        }

        @Test
        @DisplayName("status not PENDING → throws")
        void notPending_throws() {
            when(reservationRepository.findById(2L)).thenReturn(Optional.of(confirmedReservation));

            assertThatThrownBy(() -> reservationService.confirmReservation(2L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Only pending reservations can be confirmed");
        }

        @Test
        @DisplayName("status PENDING → confirmed and saved")
        void pending_confirmed() {
            when(reservationRepository.findById(1L)).thenReturn(Optional.of(pendingReservation));
            when(reservationRepository.save(pendingReservation)).thenReturn(pendingReservation);

            ReservationResponse result = reservationService.confirmReservation(1L);

            assertThat(pendingReservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("checkIn()")
    class CheckIn {

        @Test
        @DisplayName("reservation NOT found → throws")
        void notFound_throws() {
            when(reservationRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.checkIn(99L, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Reservation not found");
        }

        @Test
        @DisplayName("status not CONFIRMED → throws")
        void notConfirmed_throws() {
            when(reservationRepository.findById(1L)).thenReturn(Optional.of(pendingReservation));

            assertThatThrownBy(() -> reservationService.checkIn(1L, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Only confirmed reservations can be checked in");
        }

        @Test
        @DisplayName("status CONFIRMED → marks as SEATED and tables OCCUPIED")
        void confirmed_checksIn() {
            when(reservationRepository.findById(2L)).thenReturn(Optional.of(confirmedReservation));
            when(reservationRepository.save(confirmedReservation)).thenReturn(confirmedReservation);

            ReservationResponse result = reservationService.checkIn(2L, 1L);

            assertThat(table.getStatus()).isEqualTo(TableStatus.OCCUPIED);
            assertThat(result).isNotNull();
            verify(reservationRepository).save(confirmedReservation);
        }
    }

    @Nested
    @DisplayName("cancelReservation()")
    class CancelReservation {

        @Test
        @DisplayName("reservation NOT found → throws")
        void notFound_throws() {
            when(reservationRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.cancelReservation(99L, "reason"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Reservation not found");
        }

        @Test
        @DisplayName("status COMPLETED → throws")
        void completed_throws() {
            when(reservationRepository.findById(4L)).thenReturn(Optional.of(completedReservation));

            assertThatThrownBy(() -> reservationService.cancelReservation(4L, "reason"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Cannot cancel completed or no-show");
        }

        @Test
        @DisplayName("status NO_SHOW → throws")
        void noShow_throws() {
            when(reservationRepository.findById(5L)).thenReturn(Optional.of(noShowReservation));

            assertThatThrownBy(() -> reservationService.cancelReservation(5L, "reason"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Cannot cancel completed or no-show");
        }

        @Test
        @DisplayName("status PENDING → cancelled, tables freed")
        void pending_cancelled() {
            when(reservationRepository.findById(1L)).thenReturn(Optional.of(pendingReservation));
            when(reservationRepository.save(pendingReservation)).thenReturn(pendingReservation);

            ReservationResponse result = reservationService.cancelReservation(1L, "Customer request");

            assertThat(pendingReservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
            assertThat(pendingReservation.getCancellationReason()).isEqualTo("Customer request");
            assertThat(pendingReservation.getCancelledAt()).isNotNull();
            assertThat(table.getStatus()).isEqualTo(TableStatus.AVAILABLE);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("status CONFIRMED → can also be cancelled")
        void confirmed_cancelled() {
            when(reservationRepository.findById(2L)).thenReturn(Optional.of(confirmedReservation));
            when(reservationRepository.save(confirmedReservation)).thenReturn(confirmedReservation);

            reservationService.cancelReservation(2L, "No reason");

            assertThat(confirmedReservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        }
    }

    @Nested
    @DisplayName("markAsNoShow()")
    class MarkAsNoShow {

        @Test
        @DisplayName("reservation NOT found → throws")
        void notFound_throws() {
            when(reservationRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.markAsNoShow(99L, "reason"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Reservation not found");
        }

        @Test
        @DisplayName("reservation not eligible for no-show → throws")
        void notEligible_throws() {
            when(reservationRepository.findById(1L)).thenReturn(Optional.of(pendingReservation));

            assertThatThrownBy(() -> reservationService.markAsNoShow(1L, "reason"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not eligible for no-show");
        }

        @Test
        @DisplayName("reservation eligible → marked NO_SHOW, tables freed")
        void eligible_markedNoShow() {
            Reservation eligibleReservation = buildReservation(6L, ReservationStatus.CONFIRMED);
            eligibleReservation.setReservationTime(LocalDateTime.now().minusHours(1));
            when(reservationRepository.findById(6L)).thenReturn(Optional.of(eligibleReservation));
            when(reservationRepository.save(eligibleReservation)).thenReturn(eligibleReservation);

            reservationService.markAsNoShow(6L, "Did not arrive");

            assertThat(table.getStatus()).isEqualTo(TableStatus.AVAILABLE);
            verify(reservationRepository).save(eligibleReservation);
        }
    }

    @Nested
    @DisplayName("autoMarkNoShowReservations()")
    class AutoMarkNoShowReservations {

        @Test
        @DisplayName("no overdue reservations → nothing happens")
        void noOverdue_nothingHappens() {
            when(reservationRepository.findOverdueReservations(any())).thenReturn(Collections.emptyList());

            reservationService.autoMarkNoShowReservations();

            verify(reservationRepository, never()).save(any());
        }

        @Test
        @DisplayName("overdue reservation eligible → auto-marked as NO_SHOW")
        void overdueEligible_markedNoShow() {
            Reservation overdue = buildReservation(10L, ReservationStatus.CONFIRMED);
            overdue.setReservationTime(LocalDateTime.now().minusHours(1));

            when(reservationRepository.findOverdueReservations(any())).thenReturn(List.of(overdue));
            when(reservationRepository.findById(10L)).thenReturn(Optional.of(overdue));
            when(reservationRepository.save(overdue)).thenReturn(overdue);

            reservationService.autoMarkNoShowReservations();

            verify(reservationRepository).save(overdue);
        }

        @Test
        @DisplayName("overdue reservation NOT eligible → not marked")
        void overdueNotEligible_notMarked() {
            Reservation notEligible = buildReservation(11L, ReservationStatus.PENDING);
            notEligible.setReservationTime(LocalDateTime.now().minusHours(1));

            when(reservationRepository.findOverdueReservations(any())).thenReturn(List.of(notEligible));

            reservationService.autoMarkNoShowReservations();

            verify(reservationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getReservation()")
    class GetReservation {

        @Test
        @DisplayName("reservation found → returns response")
        void found_returnsResponse() {
            when(reservationRepository.findById(1L)).thenReturn(Optional.of(pendingReservation));

            ReservationResponse result = reservationService.getReservation(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("reservation NOT found → throws")
        void notFound_throws() {
            when(reservationRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.getReservation(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Reservation not found");
        }
    }

    @Nested
    @DisplayName("getReservationsByDateRange()")
    class GetReservationsByDateRange {

        @Test
        @DisplayName("returns mapped reservations in range")
        void returnsMappedList() {
            LocalDateTime start = LocalDateTime.now();
            LocalDateTime end = start.plusDays(7);
            when(reservationRepository.findByReservationTimeBetween(start, end))
                    .thenReturn(List.of(pendingReservation));

            List<ReservationResponse> result = reservationService.getReservationsByDateRange(start, end);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getReservationsByStatus()")
    class GetReservationsByStatus {

        @Test
        @DisplayName("returns mapped reservations by status")
        void returnsMappedList() {
            when(reservationRepository.findByStatus(ReservationStatus.PENDING))
                    .thenReturn(List.of(pendingReservation));

            List<ReservationResponse> result = reservationService.getReservationsByStatus(ReservationStatus.PENDING);

            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("mapToResponse() — computed fields")
    class MapToResponse {

        @Test
        @DisplayName("CONFIRMED reservation → canCheckIn=true, canCancel=true")
        void confirmed_canCheckInAndCancel() {
            when(reservationRepository.findById(2L)).thenReturn(Optional.of(confirmedReservation));

            ReservationResponse result = reservationService.getReservation(2L);

            assertThat(result.getCanCheckIn()).isTrue();
            assertThat(result.getCanCancel()).isTrue();
        }

        @Test
        @DisplayName("PENDING reservation → canCheckIn=false, canCancel=true")
        void pending_cannotCheckIn_canCancel() {
            when(reservationRepository.findById(1L)).thenReturn(Optional.of(pendingReservation));

            ReservationResponse result = reservationService.getReservation(1L);

            assertThat(result.getCanCheckIn()).isFalse();
            assertThat(result.getCanCancel()).isTrue();
        }

        @Test
        @DisplayName("CANCELLED reservation → canCheckIn=false, canCancel=false")
        void cancelled_cannotCheckInOrCancel() {
            when(reservationRepository.findById(3L)).thenReturn(Optional.of(cancelledReservation));

            ReservationResponse result = reservationService.getReservation(3L);

            assertThat(result.getCanCheckIn()).isFalse();
            assertThat(result.getCanCancel()).isFalse();
        }

        @Test
        @DisplayName("reservation with bill → billId mapped")
        void withBill_billIdMapped() {
            Bill bill = new Bill();
            bill.setId(7L);
            pendingReservation.setBill(bill);
            when(reservationRepository.findById(1L)).thenReturn(Optional.of(pendingReservation));

            ReservationResponse result = reservationService.getReservation(1L);

            assertThat(result.getBillId()).isEqualTo(7L);
        }

        @Test
        @DisplayName("reservation without bill → billId null")
        void withoutBill_billIdNull() {
            pendingReservation.setBill(null);
            when(reservationRepository.findById(1L)).thenReturn(Optional.of(pendingReservation));

            ReservationResponse result = reservationService.getReservation(1L);

            assertThat(result.getBillId()).isNull();
        }
    }
}