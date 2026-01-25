package com.restaurant.qrorder.domain.common;

/**
 * Enum định nghĩa cách tính giá trị discount
 */
public enum DiscountValueType {
    /**
     * Giảm theo phần trăm (%)
     * VD: value = 20 → Giảm 20%
     */
    PERCENTAGE,
    
    /**
     * Giảm số tiền cố định
     * VD: value = 10000 → Giảm 10,000đ
     */
    FIXED_AMOUNT,
    
    /**
     * Giá cố định cho món (chỉ dùng cho ITEM_SPECIFIC)
     * VD: value = 35000 → Món này giá 35,000đ (thay vì giá gốc)
     */
    FIXED_PRICE
}
