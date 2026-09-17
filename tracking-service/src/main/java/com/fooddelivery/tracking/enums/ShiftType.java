package com.fooddelivery.tracking.enums;

/**
 * Phân loại ca làm việc của Shipper
 */
public enum ShiftType {
    MORNING,    // Ca sáng (6:00–12:00)
    AFTERNOON,  // Ca chiều (12:00–18:00)
    EVENING,    // Ca tối (18:00–22:00)
    CUSTOM      // Ca tự đặt giờ
}