package com.fooddelivery.orders.enums;

/**
 * Trạng thái đơn hàng theo FSM (Finite State Machine)
 */
public enum OrderStatus {
    PLACED,            // Đã đặt, chờ Shop xác nhận
    CONFIRMED,         // Shop xác nhận, bắt đầu nấu
    PREPARING,         // Đang chuẩn bị / nấu
    READY_FOR_PICKUP,  // Xong, chờ Shipper đến lấy
    ASSIGNED,          // Đã ghép Shipper
    PICKED_UP,         // Shipper đã lấy hàng
    DELIVERING,        // Đang trên đường giao
    DELIVERED,         // Đã giao đến tay khách
    COMPLETED,         // Hoàn tất (sau xác nhận / timeout tự hoàn)
    CANCELLED          // Đã huỷ
}