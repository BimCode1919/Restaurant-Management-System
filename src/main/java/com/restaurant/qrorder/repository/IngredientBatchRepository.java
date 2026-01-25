package com.restaurant.qrorder.repository;

import com.restaurant.qrorder.domain.entity.IngredientBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IngredientBatchRepository extends JpaRepository<IngredientBatch, Long> {

    List<IngredientBatch> findByIngredientIdOrderByCreatedAtDesc(Long ingredientId);

    @Query("SELECT b FROM IngredientBatch b WHERE b.expiryDate < :date ORDER BY b.expiryDate ASC")
    List<IngredientBatch> findExpiredBatches(@Param("date") LocalDateTime date);

    @Query("SELECT b FROM IngredientBatch b WHERE b.expiryDate BETWEEN :start AND :end ORDER BY b.expiryDate ASC")
    List<IngredientBatch> findExpiringBatches(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
