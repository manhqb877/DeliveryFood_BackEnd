package com.fooddelivery.tracking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryResponse {

    private Long id;
    private Long orderId;
    private String orderCode;
    private Long shipperId;
    private Long areaId;

    // Tọa độ lấy hàng (Quán)
    private Double pickupLat;
    private Double pickupLng;
    private String pickupAddress;

    // Tọa độ giao hàng (Khách)
    private Double deliveryLat;
    private Double deliveryLng;
    private String deliveryAddress;

    // Chi tiết địa chỉ giao
    private String deliveryBuilding;
    private String deliveryFloor;
    private String deliveryUnit;
    private String deliveryGate;

    // Ước tính
    private Integer estimatedDistanceM;
    private Integer estimatedDurationS;
    private Short currentEtaMinutes;

    // Thanh toán
    private BigDecimal codAmount;
    private Boolean codCollected;

    // Trạng thái & xác nhận
    private String status;
    private String confirmMethod;
    private String deliveryOtp;
    private String proofPhotoUrl;

    // Mốc thời gian
    private OffsetDateTime assignedAt;
    private OffsetDateTime pickedUpAt;
    private OffsetDateTime deliveredAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
