package com.restaurant.qrorder.repository;

import com.restaurant.qrorder.domain.common.TableStatus;
import com.restaurant.qrorder.domain.entity.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long> {

    Optional<RestaurantTable> findByTableNumber(String tableNumber);

    List<RestaurantTable> findByStatus(TableStatus status);

    Optional<RestaurantTable> findByQrCode(String qrCode);

    // Find available tables (not reserved or occupied)
    @Query("SELECT t FROM RestaurantTable t WHERE t.status = 'AVAILABLE'")
    List<RestaurantTable> findAvailableTables();

    // Find tables available for a specific time period
    @Query("SELECT t FROM RestaurantTable t WHERE t.status = 'AVAILABLE' " +
           "AND t.id NOT IN (" +
           "  SELECT rt.id FROM Reservation r JOIN r.tables rt " +
           "  WHERE r.status IN ('CONFIRMED', 'SEATED') " +
           "  AND r.reservationTime BETWEEN :start AND :end" +
           ")")
    List<RestaurantTable> findAvailableTablesForTimePeriod(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
