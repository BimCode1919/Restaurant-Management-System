package com.restaurant.qrorder.domain.entity;

import com.restaurant.qrorder.domain.common.DiscountType;
import com.restaurant.qrorder.domain.common.DiscountValueType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "discounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false, length = 100)
    private String code;

    @Column(name = "discount_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    @Column(name = "value_type", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DiscountValueType valueType = DiscountValueType.PERCENTAGE;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal value;

    @Column(name = "min_order_amount", precision = 10, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(name = "max_discount_amount", precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(length = 500)
    private String description;

    @Column(name = "min_party_size")
    private Integer minPartySize;

    @Column(name = "max_party_size")
    private Integer maxPartySize;

    @Column(name = "tier_config", columnDefinition = "TEXT")
    private String tierConfig;

    @Column(name = "applicable_days", columnDefinition = "TEXT")
    private String applicableDays;

    @Column(name = "apply_to_specific_items")
    @Builder.Default
    private Boolean applyToSpecificItems = false;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "used_count")
    @Builder.Default
    private Integer usedCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @ManyToMany(mappedBy = "discounts")
    @Builder.Default
    private List<Item> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
