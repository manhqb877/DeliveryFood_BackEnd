package com.fooddelivery.core.enums;


/**
 * Đối tượng được phép áp dụng khuyến mãi
 */
public enum ApplicableTo {
    ALL,       // Tất cả (Guest và Khách hàng)
    CUSTOMER,  // Chỉ Khách hàng đã đăng ký
    RESIDENT,  // Chỉ cư dân đã xác thực khu vực
    NEW_USER   // Chỉ người dùng đặt lần đầu
}