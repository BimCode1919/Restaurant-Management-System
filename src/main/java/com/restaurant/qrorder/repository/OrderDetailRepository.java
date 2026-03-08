package com.restaurant.qrorder.repository;

import com.restaurant.qrorder.domain.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    @Query("SELECT od FROM OrderDetail od JOIN FETCH od.order o JOIN FETCH od.item WHERE 1=1 ORDER BY o.createdAt ASC")
    List<OrderDetail> findAllSortedByOrderCreatedAt();
}
