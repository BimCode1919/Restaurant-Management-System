package com.restaurant.qrorder.util;

import com.restaurant.qrorder.domain.entity.Item;
import org.springframework.data.jpa.domain.Specification;

public class ItemSpecification {
    public static Specification<Item>getItemByIdAndName(String name)
    {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("name"), "%"+name+"%");

    }
}
