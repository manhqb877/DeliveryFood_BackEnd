package com.fooddelivery.analyticsservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class FraudAlertDto {
    private String id;
    private String alertType;
    private String severity;
    private Long userId;
    private Long guestSessionId;
    private String ipAddress;
    private String deviceFingerprint;
    private Double detectedLat;
    private Double detectedLng;
    private String description;
    private String status;
    private Long reviewedBy;
    private Instant reviewedAt;
    private String reviewNote;
    private Instant createdAt;
    private Long orderId;
    private Long shipperId;
}
