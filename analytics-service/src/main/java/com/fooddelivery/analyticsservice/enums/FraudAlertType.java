package com.fooddelivery.analyticsservice.enums;


/**
 * Phân loại cảnh báo gian lận từ hệ thống AI / Fraud Detection
 */
public enum FraudAlertType {
    PROMO_ABUSE,            // Lạm dụng khuyến mãi
    FAKE_ORDER,             // Đơn hàng ảo (đặt rồi huỷ liên tục)
    MASS_ACCOUNT_CREATION,  // Nhiều tài khoản từ 1 thiết bị/IP
    SUSPICIOUS_LOCATION,    // Vị trí bất thường
    VELOCITY_FRAUD,         // Đặt quá nhiều đơn trong thời gian ngắn
    SHOP_FRAUD,             // Quán bị đánh giá thấp
    SHIPPER_FRAUD           // Shipper bị đánh giá thấp
}