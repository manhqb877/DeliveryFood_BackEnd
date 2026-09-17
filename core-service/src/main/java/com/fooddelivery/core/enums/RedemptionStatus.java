package com.fooddelivery.core.enums;

/**
 * Trạng thái áp dụng / lịch sử sử dụng khuyến mãi
 */
public enum RedemptionStatus {
    LOCKED,   // Soft-lock khi thêm vào giỏ (trước khi đặt xong)
    USED,     // Đã dùng thành công
    RELEASED  // Đã giải phóng (đơn huỷ / timeout)
}