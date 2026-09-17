package com.fooddelivery.notification.enums;

/**
 * Phân loại nội dung thông báo
 */
public enum NotificationType {
    ORDER_STATUS,     // Thay đổi trạng thái đơn
    DELIVERY_OTP,     // OTP xác nhận giao hàng
    PROMOTION,        // Khuyến mãi mới
    SYSTEM,           // Thông báo hệ thống
    CHAT_MESSAGE,     // Tin nhắn chat mới
    COMPLAINT_UPDATE  // Cập nhật khiếu nại
}