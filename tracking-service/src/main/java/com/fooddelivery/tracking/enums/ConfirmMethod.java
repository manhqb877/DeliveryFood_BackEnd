package com.fooddelivery.tracking.enums;

/**
 * Phương thức xác nhận hoàn thành giao hàng
 */
public enum ConfirmMethod {
    OTP,        // Khách đọc mã OTP cho Shipper
    PHOTO,      // Shipper chụp ảnh bằng chứng
    SIGNATURE   // Khách ký điện tử
}