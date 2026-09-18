package com.fooddelivery.orders.enums;
/**
 * Trạng thái điều phối Saga Orchestrator
 */
public enum SagaStatus {
    IN_PROGRESS,   // Saga đang chạy
    COMPENSATING,  // Đang hoàn tác (bước nào đó thất bại)
    COMPLETED,     // Saga hoàn tất thành công
    FAILED         // Saga và compensating đều thất bại
}