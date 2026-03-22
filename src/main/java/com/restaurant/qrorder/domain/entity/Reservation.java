package com.restaurant.qrorder.domain.entity;

import com.restaurant.qrorder.domain.common.ReservationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reservations", indexes = {
    @Index(name = "idx_reservation_time", columnList = "reservation_time"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_customer_phone", columnList = "customer_phone")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_phone", nullable = false, length = 20)
    private String customerPhone;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(name = "party_size", nullable = false)
    private Integer partySize;

    @Column(name = "reservation_time", nullable = false)
    private LocalDateTime reservationTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String note;

    // Deposit amount (if required)
    @Column(name = "deposit_required")
    @Builder.Default
    private Boolean depositRequired = false;

    @Column(name = "deposit_amount", precision = 10, scale = 2)
    private java.math.BigDecimal depositAmount;

    @Column(name = "deposit_paid")
    @Builder.Default
    private Boolean depositPaid = false;

    // Thời gian chờ trước khi mark NO_SHOW (minutes)
    @Column(name = "grace_period_minutes")
    @Builder.Default
    private Integer gracePeriodMinutes = 15;

    // Thời gian thực tế khách đến
    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime;

    // Thời gian hủy hoặc no-show
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    // Reference to Bill when customer is seated
    @OneToOne(mappedBy = "reservation")
    private Bill bill;

    // Pre-orders (đặt món trước)
    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Order> preOrders = new ArrayList<>();

    // Reserved tables
    @ManyToMany
    @JoinTable(
        name = "reservation_tables",
        joinColumns = @JoinColumn(name = "reservation_id"),
        inverseJoinColumns = @JoinColumn(name = "table_id")
    )
    @Builder.Default
    private List<RestaurantTable> tables = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public boolean isNoShowEligible() {
        if (reservationTime == null || status != ReservationStatus.CONFIRMED) {
            return false;
        }
        LocalDateTime noShowTime = reservationTime.plusMinutes(gracePeriodMinutes);
        return LocalDateTime.now().isAfter(noShowTime);
    }

    public void markAsNoShow(String reason) {
        this.status = ReservationStatus.NO_SHOW;
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = reason;
    }

    public void markAsSeated() {
        this.status = ReservationStatus.SEATED;
        this.arrivalTime = LocalDateTime.now();
    }
}
