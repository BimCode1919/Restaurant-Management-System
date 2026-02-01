package com.restaurant.qrorder.repository;

import com.restaurant.qrorder.domain.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    List<Order> findByBillId(Long billId);
}
