package com.fooddelivery.orders.enums;

public enum PaymentStatus {
    PENDING,      // Chờ thanh toán
    PAID,         // Đã thanh toán online
    FAILED,       // Thanh toán thất bại
    REFUNDED,     // Đã hoàn tiền
    COD_PENDING   // COD — shipper chưa thu tiền
}