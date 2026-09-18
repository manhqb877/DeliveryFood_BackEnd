package com.fooddelivery.orders.enums;

/**
 * Phân loại tác giả viết bình luận
 */
public enum AuthorType {
    CUSTOMER,      // Khách hàng đã đăng ký
    GUEST,         // Khách vãng lai
    SHOP_MANAGER,  // Phản hồi của Shop Manager
    ADMIN          // Admin hệ thống
}