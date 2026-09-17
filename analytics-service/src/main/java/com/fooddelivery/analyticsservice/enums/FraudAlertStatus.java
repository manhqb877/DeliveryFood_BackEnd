package com.fooddelivery.analyticsservice.enums;

/**
 * Trạng thái xử lý cảnh báo gian lận
 */
public enum FraudAlertStatus {
    OPEN,           // Mới phát sinh, chờ kiểm tra
    INVESTIGATING,  // Đang điều tra
    CONFIRMED,      // Đã xác nhận gian lận
    FALSE_POSITIVE, // Báo động giả
    RESOLVED        // Đã xử lý xong
}
