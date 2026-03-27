package com.restaurant.qrorder.domain.entity;

import com.restaurant.qrorder.domain.common.BillStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bills")
@Getter
@Setter
@Builder
@ToString(exclude = {"reservation","payments","orders","billTables"})
@NoArgsConstructor
@AllArgsConstructor
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @Column(name = "party_size")
    private Integer partySize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_id")
    private Discount discount;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "final_price", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal finalPrice = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BillStatus status = BillStatus.OPEN;

    // Link to reservation if this bill is from a reservation
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    // Payment information — a bill can have a deposit payment + a final payment
    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BillTable> billTables = new ArrayList<>();

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Order> orders = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "bill_previous_ids", joinColumns = @JoinColumn(name = "bill_id"))
    @Column(name = "previous_bill_id")
    @Builder.Default
    private List<Long> previousBillIds = new ArrayList<>();

    // Helper method to get table numbers
    public List<String> getTableNumbers() {
        return billTables.stream()
                .map(bt -> bt.getTable().getTableNumber())
                .toList();
    }
}
