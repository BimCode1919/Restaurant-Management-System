package com.restaurant.qrorder.repository;

import com.restaurant.qrorder.domain.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    List<Recipe> findByItemId(Long itemId);
}
