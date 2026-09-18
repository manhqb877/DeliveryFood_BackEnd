package com.fooddelivery.tracking.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "deliveries", indexes = {
        @Index(name = "idx_deliveries_order", columnList = "order_id"),
        @Index(name = "idx_deliveries_shipper", columnList = "shipper_id, status"),
        @Index(name = "idx_deliveries_area", columnList = "area_id, status"),
        @Index(name = "idx_deliveries_batch", columnList = "batch_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId; // ID đơn hàng (1 order = 1 delivery)

    @Column(name = "area_id", nullable = false)
    private Long areaId; // ID khu vực

    @Column(name = "shipper_id")
    private Long shipperId; // ID Shipper (NULL cho đến khi ghép được)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id")
    private ShipperShift shift; // Ca làm việc tương ứng

    // Tọa độ điểm lấy hàng (Gian hàng)
    @Column(name = "pickup_lat", nullable = false)
    private Double pickupLat;

    @Column(name = "pickup_lng", nullable = false)
    private Double pickupLng;

    @Column(name = "pickup_address", nullable = false, columnDefinition = "TEXT")
    private String pickupAddress;

    // Tọa độ điểm giao hàng (Khách hàng)
    @Column(name = "delivery_lat", nullable = false)
    private Double deliveryLat;

    @Column(name = "delivery_lng", nullable = false)
    private Double deliveryLng;

    @Column(name = "delivery_address", nullable = false, columnDefinition = "TEXT")
    private String deliveryAddress;

    // Định tuyến nội khu
    @Column(name = "pickup_node_id")
    private Long pickupNodeId;

    @Column(name = "delivery_node_id")
    private Long deliveryNodeId;

    @Column(name = "delivery_building", length = 100)
    private String deliveryBuilding;

    @Column(name = "delivery_floor", length = 10)
    private String deliveryFloor;

    @Column(name = "delivery_unit", length = 50)
    private String deliveryUnit;

    @Column(name = "delivery_gate", length = 50)
    private String deliveryGate;

    // Ước tính
    @Column(name = "estimated_distance_m")
    private Integer estimatedDistanceM; // Mét

    @Column(name = "estimated_duration_s")
    private Integer estimatedDurationS; // Giây

    @Column(name = "current_eta_minutes")
    private Short currentEtaMinutes; // Phút

    @Enumerated(EnumType.STRING)
    @Column(name = "confirm_method", nullable = false, length = 20)
    private ConfirmMethod confirmMethod = ConfirmMethod.OTP;

    @Column(name = "delivery_otp", length = 10)
    private String deliveryOtp; // Mã OTP 6 số xác nhận

    @Column(name = "proof_photo_url", columnDefinition = "TEXT")
    private String proofPhotoUrl; // Ảnh chụp bằng chứng

    // Batch nhiều đơn
    @Column(name = "batch_id")
    private Long batchId;

    @Column(name = "batch_sequence")
    private Short batchSequence;

    // COD
    @Column(name = "cod_amount", precision = 12, scale = 2)
    private BigDecimal codAmount;

    @Column(name = "cod_collected")
    private Boolean codCollected = false;

    @Column(name = "cod_collected_at")
    private OffsetDateTime codCollectedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeliveryStatus status = DeliveryStatus.PENDING;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    // Mốc thời gian
    @Column(name = "assigned_at")
    private OffsetDateTime assignedAt;

    @Column(name = "going_pickup_at")
    private OffsetDateTime goingPickupAt;

    @Column(name = "picked_up_at")
    private OffsetDateTime pickedUpAt;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    @Column(name = "failed_at")
    private OffsetDateTime failedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}