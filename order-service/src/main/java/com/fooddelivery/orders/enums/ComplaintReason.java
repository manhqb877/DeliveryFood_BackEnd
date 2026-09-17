package com.fooddelivery.orders.enums;

/**
 * Loại nguyên nhân khiếu nại đơn hàng
 */
public enum ComplaintReason {
    WRONG_ITEM,     // Giao sai món
    MISSING_ITEM,   // Thiếu món
    FOOD_QUALITY,   // Chất lượng kém
    LATE_DELIVERY,  // Giao chậm
    RUDE_SHIPPER,   // Thái độ Shipper
    PAYMENT_ISSUE,  // Vấn đề thanh toán
    OTHER           // Khác
}