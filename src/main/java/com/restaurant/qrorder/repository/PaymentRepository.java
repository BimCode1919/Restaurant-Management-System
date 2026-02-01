package com.restaurant.qrorder.repository;

import com.restaurant.qrorder.domain.common.PaymentStatus;
import com.restaurant.qrorder.domain.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByBillId(Long billId);

    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByMomoOrderId(String momoOrderId);

    List<Payment> findByStatus(PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' " +
           "AND p.createdAt < CURRENT_TIMESTAMP - 15 MINUTE")
    List<Payment> findExpiredPendingPayments();
}
