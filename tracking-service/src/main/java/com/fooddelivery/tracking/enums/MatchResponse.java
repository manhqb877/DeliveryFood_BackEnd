package com.fooddelivery.tracking.enums;

/**
 * Phản hồi của Shipper khi được offer đơn hàng
 */
public enum MatchResponse {
    ACCEPTED,  // Shipper chấp nhận
    REJECTED,  // Từ chối
    TIMEOUT    // Không phản hồi trong 30s
}