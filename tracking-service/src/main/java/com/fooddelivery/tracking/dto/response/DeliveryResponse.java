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

    // Tọa độ lấy hàng (Quán 1)
    private Double pickupLat;
    private Double pickupLng;
    private String pickupAddress;
    private Long shop1Id;
    private String shop1Name;
    private Boolean pickup1Completed;

    // Tọa độ lấy hàng 2 (Quán 2 - Đơn ghép 2 quán)
    private Boolean isMultiShop;
    private Double pickup2Lat;
    private Double pickup2Lng;
    private String pickup2Address;
    private Long shop2Id;
    private String shop2Name;
    private Boolean pickup2Completed;
    private String optimalPickupOrder; // "SHOP1_FIRST" | "SHOP2_FIRST"

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
    private Boolean shop1Confirmed;
    private Boolean shop2Confirmed;
    private Boolean readyForShipper;

    // Mốc thời gian
    private OffsetDateTime assignedAt;
    private OffsetDateTime pickedUpAt;
    private OffsetDateTime deliveredAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
