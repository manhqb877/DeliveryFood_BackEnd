package com.fooddelivery.notification.enums;


/**
 * Kênh truyền tải thông báo
 */
public enum NotificationChannel {
    PUSH,   // Firebase push (Khách hàng đã login)
    SMS,    // SMS (Guest hoặc thông báo khẩn)
    EMAIL,  // Email
    IN_APP  // Trong app (không qua push)
}