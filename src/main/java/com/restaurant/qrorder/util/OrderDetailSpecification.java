package com.restaurant.qrorder.util;

import com.restaurant.qrorder.domain.common.ItemStatus;
import com.restaurant.qrorder.domain.entity.OrderDetail;
import org.springframework.data.jpa.domain.Specification;

public class OrderDetailSpecification {
    public Specification<OrderDetail> getItemByStatus(ItemStatus status)
    {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("itemStatus"), status );
    }

}
