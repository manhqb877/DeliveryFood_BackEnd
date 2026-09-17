package com.fooddelivery.tracking.enums;

/**
 * Phân loại sự cố giao hàng do Shipper báo cáo
 */
public enum IncidentType {
    CUSTOMER_UNREACHABLE,  // Khách không nghe máy
    WRONG_ADDRESS,         // Địa chỉ sai
    FOOD_DAMAGED,          // Hàng hỏng / đổ vỡ
    CUSTOMER_REFUSED,      // Khách từ chối nhận
    TRAFFIC_ISSUE,         // Không vào được khu
    ACCIDENT,              // Tai nạn
    OTHER                  // Khác
}