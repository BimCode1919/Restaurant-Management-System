package com.restaurant.qrorder.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "bill_tables")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillTable {

    @EmbeddedId
    private BillTableId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("billId")
    @JoinColumn(name = "bill_id")
    private Bill bill;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tableId")
    @JoinColumn(name = "table_id")
    private RestaurantTable table;

    @Column(name = "original_bill_id")
    private Long originalBillId;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BillTableId implements Serializable {
        private Long billId;
        private Long tableId;
    }
}
