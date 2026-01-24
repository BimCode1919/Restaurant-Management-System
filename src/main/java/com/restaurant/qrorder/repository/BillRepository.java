package com.restaurant.qrorder.repository;

import com.restaurant.qrorder.domain.common.BillStatus;
import com.restaurant.qrorder.domain.entity.Bill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    @Query("SELECT DISTINCT b FROM Bill b LEFT JOIN FETCH b.billTables bt LEFT JOIN FETCH bt.table LEFT JOIN FETCH b.orders WHERE b.id = :id")
    Optional<Bill> findByIdWithDetails(Long id);

    List<Bill> findByStatus(BillStatus status);

    Page<Bill> findByStatus(BillStatus status, Pageable pageable);

    @Query("SELECT b FROM Bill b JOIN b.billTables bt WHERE b.status = 'OPEN' AND bt.table.id = :tableId")
    Optional<Bill> findOpenBillByTableId(@Param("tableId") Long tableId);

    @Query("SELECT b FROM Bill b WHERE b.createdAt BETWEEN :startDate AND :endDate")
    List<Bill> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT b FROM Bill b WHERE b.status = 'PAID' AND " +
           "b.closedAt BETWEEN :startDate AND :endDate ORDER BY b.closedAt DESC")
    List<Bill> findPaidBillsInDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COUNT(b) FROM Bill b WHERE b.status = 'OPEN'")
    Long countOpenBills();
}
