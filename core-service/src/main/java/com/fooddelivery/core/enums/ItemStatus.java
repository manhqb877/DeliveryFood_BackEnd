package com.fooddelivery.core.enums;

/**
 * Trạng thái của món ăn trong gian hàng
 */
public enum ItemStatus {
    AVAILABLE,    // Còn món, có thể đặt
    SOLD_OUT,     // Hết món tạm thời (Shop tự cập nhật)
    HIDDEN,       // Ẩn khỏi menu (Shop tạm ẩn)
    DISCONTINUED  // Ngừng bán vĩnh viễn
}