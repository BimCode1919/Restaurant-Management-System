package com.restaurant.qrorder.domain.common;

public enum PaymentStatus {
    PENDING,        // Đang chờ thanh toán
    PROCESSING,     // Đang xử lý (cho online payment)
    COMPLETED,      // Đã thanh toán thành công
    FAILED,         // Thanh toán thất bại
    REFUNDED        // Đã hoàn tiền
}
