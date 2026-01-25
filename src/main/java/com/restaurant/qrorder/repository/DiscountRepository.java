package com.restaurant.qrorder.repository;

import com.restaurant.qrorder.domain.entity.Discount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Long> {

    List<Discount> findByActive(Boolean active);

    @Query("SELECT d FROM Discount d WHERE d.active = true AND " +
           "(d.startDate IS NULL OR d.startDate <= :now) AND " +
           "(d.endDate IS NULL OR d.endDate >= :now) AND " +
           "(d.usageLimit IS NULL OR d.usedCount < d.usageLimit)")
    List<Discount> findActiveDiscounts(@Param("now") LocalDateTime now);
}
