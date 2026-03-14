package com.restaurant.qrorder.repository;

import com.restaurant.qrorder.domain.entity.Item;
import com.restaurant.qrorder.util.ItemSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

    @Query("""
            SELECT i FROM Item i
            JOIN FETCH i.category
            """)
    List<Item> findAllWithCategory();

    Page<Item> findByCategoryId(Long categoryId, Pageable pageable);

    @Query("SELECT i FROM Item i WHERE i.available = true ORDER BY i.name ASC")
    List<Item> findAllAvailableOrderByName();

    @Query("SELECT i FROM Item i WHERE " +
           "LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(i.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Item> searchItems(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT i FROM Item i WHERE i.category.id = :categoryId AND i.available = true")
    List<Item> findAvailableItemsByCategory(Long categoryId);

}
