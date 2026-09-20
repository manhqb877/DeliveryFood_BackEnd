package com.fooddelivery.auth.dto.response;

import com.fooddelivery.auth.enums.ApprovalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopProfileResponse {
    private Long id;
    private Long ownerId;
    private String ownerName;
    private String ownerPhone;
    private String ownerEmail;
    private Long areaId;
    private String areaName;
    private String locationDetail;
    private Double shopLat;
    private Double shopLng;
    private String shopName;
    private String shopDescription;
    private String logoUrl;
    private String coverImageUrl;
    private String phone;
    private List<Map<String, Object>> businessHours;
    private ApprovalStatus approvalStatus;
    private String rejectionReason;
    private Boolean isOpen;
    private Boolean isAcceptingOrders;
    private BigDecimal commissionRate;
    private List<Map<String, Object>> documents;
    private Long approvedBy;
    private OffsetDateTime approvedAt;
    private OffsetDateTime createdAt;
}
