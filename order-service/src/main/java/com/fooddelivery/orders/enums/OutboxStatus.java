package com.fooddelivery.orders.enums;

/**
 * Trạng thái xử lý sự kiện trong Outbox Table
 */
public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}