package com.fooddelivery.tracking.enums;

/**
 * Trạng thái hành trình giao hàng (Delivery Status FSM)
 */
public enum DeliveryStatus {
    PENDING,         // Đang tìm Shipper
    MATCHING,        // Đã gửi thông báo, chờ Shipper chấp nhận (timeout 30s)
    ASSIGNED,        // Shipper chấp nhận
    GOING_PICKUP,    // Shipper đang đến gian hàng
    AT_SHOP,         // Shipper đến gian hàng, chờ lấy hàng
    PICKED_UP,       // Đã nhận hàng từ gian hàng
    DELIVERING,      // Đang giao đến khách
    AT_DESTINATION,  // Đến điểm giao, chờ xác nhận
    DELIVERED,       // Giao thành công
    FAILED,          // Giao thất bại (không gặp khách, sai địa chỉ)
    CANCELLED        // Huỷ (trước ASSIGNED)
}