package com.fooddelivery.notification.enums;

/**
 * Phân loại luồng hội thoại chat
 */
public enum ConversationType {
    CUSTOMER_SHOP,     // Khách hàng ↔ Shop Manager
    CUSTOMER_SHIPPER,  // Khách hàng ↔ Shipper
    SUPPORT,           // Hỗ trợ hệ thống
    GUEST_SHOP         // Guest ↔ Shop Manager
}
