package com.restaurant.qrorder.domain.entity;

import com.restaurant.qrorder.domain.common.PaymentMethod;
import com.restaurant.qrorder.domain.common.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_transaction_id", columnList = "transaction_id"),
    @Index(name = "idx_payment_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    // For online payments (MoMo, bank transfer, etc.)
    @Column(name = "transaction_id", unique = true)
    private String transactionId;

    @Column(name = "payment_url", columnDefinition = "TEXT")
    private String paymentUrl;

    // MoMo specific fields
    @Column(name = "momo_order_id")
    private String momoOrderId;

    @Column(name = "momo_request_id")
    private String momoRequestId;

    @Column(name = "momo_trans_id")
    private String momoTransId;

    // Response data from payment gateway
    @Column(name = "gateway_response", columnDefinition = "TEXT")
    private String gatewayResponse;

    @Column(name = "error_message")
    private String errorMessage;

    // Timestamps
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "refund_reason")
    private String refundReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public void markAsPaid() {
        this.status = PaymentStatus.COMPLETED;
        this.paidAt = LocalDateTime.now();
    }

    public void markAsFailed(String errorMessage) {
        this.status = PaymentStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public void processRefund(BigDecimal refundAmount, String reason) {
        this.status = PaymentStatus.REFUNDED;
        this.refundedAt = LocalDateTime.now();
        this.refundAmount = refundAmount;
        this.refundReason = reason;
    }

    public boolean isCashPayment() {
        return PaymentMethod.CASH.equals(this.method);
    }

    public boolean isOnlinePayment() {
        return PaymentMethod.MOMO.equals(this.method) || 
               PaymentMethod.BANK_TRANSFER.equals(this.method) ||
               PaymentMethod.CREDIT_CARD.equals(this.method);
    }
}
