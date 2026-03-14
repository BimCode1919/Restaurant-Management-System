package com.restaurant.qrorder.repository;

import com.restaurant.qrorder.domain.entity.IngredientUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientUsageRepository extends JpaRepository<IngredientUsage, Long> {
}
