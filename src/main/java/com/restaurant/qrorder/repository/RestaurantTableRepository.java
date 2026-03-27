package com.restaurant.qrorder.repository;

import com.restaurant.qrorder.domain.common.TableStatus;
import com.restaurant.qrorder.domain.entity.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long> {

    @Query("SELECT t FROM RestaurantTable t ORDER BY t.tableNumber ASC")
    List<RestaurantTable> findAllTables();

    Optional<RestaurantTable> findByTableNumber(String tableNumber);

    List<RestaurantTable> findByStatus(TableStatus status);

    Optional<RestaurantTable> findByQrCode(String qrCode);

    // Find available tables (not reserved or occupied)
    @Query("SELECT t FROM RestaurantTable t WHERE t.status = 'AVAILABLE'")
    List<RestaurantTable> findAvailableTables();

    // Find tables available for a specific time period (time-based conflict check, not status-based)
    @Query("SELECT t FROM RestaurantTable t WHERE t.status != 'MAINTENANCE' " +
           "AND t.id NOT IN (" +
           "  SELECT rt.id FROM Reservation r JOIN r.tables rt " +
           "  WHERE r.status IN ('PENDING', 'CONFIRMED', 'SEATED') " +
           "  AND r.reservationTime < :end " +
           "  AND r.reservationTime > :startMinus2h" +
           ")")
    List<RestaurantTable> findAvailableTablesForTimePeriod(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("startMinus2h") LocalDateTime startMinus2h
    );

    @Query("""
        SELECT DISTINCT t FROM Reservation r
        JOIN r.tables t
        WHERE r.reservationTime BETWEEN :start AND :end
        AND r.status NOT IN ('PENDING', 'SEATED', 'CONFIRMED')
    """)
    List<RestaurantTable> findReservedTablesForTimePeriod(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query(value = """
    SELECT t.* FROM tables t
    WHERE t.status != 'MAINTENANCE'
    AND t.id NOT IN (
        SELECT rt.table_id
        FROM reservation_tables rt
        JOIN reservations r ON r.id = rt.reservation_id
        WHERE r.status NOT IN ('CANCELLED', 'NO_SHOW', 'COMPLETED')
        AND DATE(r.reservation_time) = DATE(:reservationDate)
        AND (
            -- Direct overlap
            (r.start_time < :endTime AND :startTime < r.end_time)
            OR
            -- 2-hour buffer before: new end within 2hrs of existing start
            (EXTRACT(EPOCH FROM (r.start_time - CAST(:endTime AS TIME))) / 60 < 120
                AND CAST(:endTime AS TIME) <= r.start_time)
            OR
            -- 2-hour buffer after: new start within 2hrs of existing end
            (EXTRACT(EPOCH FROM (CAST(:startTime AS TIME) - r.end_time)) / 60 < 120
                AND CAST(:startTime AS TIME) >= r.end_time)
        )
    )
    ORDER BY t.capacity ASC
""", nativeQuery = true)
    List<RestaurantTable> findAvailableTables(
            @Param("reservationDate") LocalDateTime reservationDate,
            @Param("startTime")       LocalTime startTime,
            @Param("endTime")         LocalTime endTime
    );



}
