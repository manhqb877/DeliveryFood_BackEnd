package com.fooddelivery.core.enums;


/**
 * Phân loại bảng giá theo thời điểm / mùa
 */
public enum PriceType {
    NORMAL,      // Giá bình thường (mặc định khi không có rule nào khác)
    PEAK_HOUR,   // Giá giờ cao điểm (cơm trưa, bữa tối)
    OFF_PEAK,    // Giá giờ thấp điểm (ưu đãi để kéo khách)
    SEASONAL,    // Giá theo mùa (Tết, lễ, mùa mưa...)
    WEEKEND,     // Giá cuối tuần (T7, CN)
    HAPPY_HOUR,  // Giờ vui vẻ (VD: 14:00–16:00 giảm giá)
    EARLY_BIRD,  // Giá đặt sớm (trước 10h sáng)
    LATE_NIGHT,  // Giá đêm khuya (sau 22h)
    PROMOTION    // Giá khuyến mãi đặc biệt do Shop tạo
}