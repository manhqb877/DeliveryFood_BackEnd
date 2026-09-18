package com.fooddelivery.tracking.enums;

/**
 * Trạng thái ca làm việc
 */
public enum ShiftStatus {
    SCHEDULED,  // Lên kế hoạch, chưa bắt đầu
    ACTIVE,     // Đang trong ca (đã bật online)
    COMPLETED,  // Ca kết thúc
    ABSENT      // Vắng mặt (có ca nhưng không bật)
}