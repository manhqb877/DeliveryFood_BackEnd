package com.fooddelivery.auth.enums;

/**
 * Trạng thái duyệt hồ sơ (Gian hàng hoặc Shipper)
 */
public enum ApprovalStatus {
    PENDING,    // Mới đăng ký, chờ Admin duyệt
    APPROVED,   // Được duyệt, có thể hoạt động
    REJECTED,   // Bị từ chối (xem rejection_reason)
    SUSPENDED   // Bị đình chỉ do vi phạm
}