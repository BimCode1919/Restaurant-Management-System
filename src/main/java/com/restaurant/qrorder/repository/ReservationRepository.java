package com.restaurant.qrorder.repository;

import com.restaurant.qrorder.domain.common.ReservationStatus;
import com.restaurant.qrorder.domain.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("SELECT DISTINCT r FROM Reservation r LEFT JOIN FETCH r.tables WHERE r.customerPhone = :customerPhone")
    List<Reservation> findByCustomerPhone(@Param("customerPhone") String customerPhone);

    @Query("SELECT DISTINCT r FROM Reservation r LEFT JOIN FETCH r.tables WHERE r.status = :status")
    List<Reservation> findByStatus(@Param("status") ReservationStatus status);

    @Query("SELECT DISTINCT r FROM Reservation r LEFT JOIN FETCH r.tables WHERE r.reservationTime BETWEEN :start AND :end")
    List<Reservation> findByReservationTimeBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT DISTINCT r FROM Reservation r LEFT JOIN FETCH r.tables WHERE r.status = :status AND r.reservationTime BETWEEN :start AND :end")
    List<Reservation> findByStatusAndReservationTimeBetween(
            @Param("status") ReservationStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Find reservations that are eligible for NO_SHOW
    @Query("SELECT r FROM Reservation r WHERE r.status = 'CONFIRMED' " +
           "AND r.reservationTime < :cutoffTime")
    List<Reservation> findOverdueReservations(@Param("cutoffTime") LocalDateTime cutoffTime);

    // Find active reservations (not cancelled, no_show, or completed)
    @Query("SELECT r FROM Reservation r WHERE r.status IN ('PENDING', 'CONFIRMED', 'SEATED')")
    List<Reservation> findActiveReservations();

    // Check if tables have overlapping reservations in the given time window.
    // Overlap condition: existing.start < newEnd AND existing.start > newStart - DINING_HOURS
    // Pass excludeId = 0L when creating (no reservation to exclude).
    @Query("SELECT r FROM Reservation r JOIN r.tables t WHERE t.id IN :tableIds " +
           "AND r.id != :excludeId " +
           "AND r.status IN ('PENDING', 'CONFIRMED', 'SEATED') " +
           "AND r.reservationTime < :end " +
           "AND r.reservationTime > :startMinus2h")
    List<Reservation> findConflictingReservations(
            @Param("tableIds") List<Long> tableIds,
            @Param("end") LocalDateTime end,
            @Param("startMinus2h") LocalDateTime startMinus2h,
            @Param("excludeId") Long excludeId
    );

    @Query("""
        SELECT r FROM Reservation r
        JOIN FETCH r.tables t
        WHERE r.status = 'CONFIRMED'
        AND r.reservationTime BETWEEN :now AND :twoHourLater
        AND t.status = 'AVAILABLE'
    """)
    List<Reservation> findReservationsToReserveTable(
            @Param("now") LocalDateTime now,
            @Param("twoHourLater") LocalDateTime twoHourLater
    );
}
