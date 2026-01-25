package com.restaurant.qrorder.domain.common;

public enum UserRole {
    ADMIN,      // Quản trị viên - full access
    STAFF,      // Nhân viên phục vụ - quản lý đơn hàng, bàn
    CHEF,       // Đầu bếp - xử lý món ăn
    CUSTOMER    // Khách hàng - đặt món
}
