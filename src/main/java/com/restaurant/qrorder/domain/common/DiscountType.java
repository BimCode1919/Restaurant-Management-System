package com.restaurant.qrorder.domain.common;

public enum DiscountType {
    ITEM_SPECIFIC,   // Giảm giá cho món cụ thể do admin set (phần trăm hoặc cố định)
    HOLIDAY,         // Giảm giá ngày lễ/thứ trong tuần (phần trăm)
    PARTY_SIZE,      // Giảm theo số người đi ăn (phần trăm)
    BILL_TIER
}
