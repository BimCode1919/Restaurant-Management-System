package com.restaurant.qrorder.domain.common;

public enum ReservationStatus {
    PENDING,        // Chờ xác nhận
    CONFIRMED,      // Đã xác nhận
    SEATED,         // Đã đến và ngồi vào bàn
    NO_SHOW,        // Không đến
    CANCELLED,      // Đã hủy
    COMPLETED       // Hoàn thành (đã thanh toán xong)
}
