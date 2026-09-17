package com.fooddelivery.auth.entity;


import com.fooddelivery.auth.enums.ApprovalStatus;
import com.fooddelivery.auth.enums.VehicleType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "shipper_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipperProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user; // Thông tin tài khoản shipper (User có role SHIPPER)

    @Column(name = "id_card_number", length = 20)
    private String idCardNumber; // Số CCCD / CMND của shipper

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", length = 20)
    private VehicleType vehicleType; // Loại phương tiện (MOTORBIKE, BICYCLE, EBIKE, WALKING)

    @Column(name = "vehicle_plate", length = 20)
    private String vehiclePlate; // Biển số xe

    @Column(name = "vehicle_photo_url", columnDefinition = "TEXT")
    private String vehiclePhotoUrl; // Ảnh chụp phương tiện

    @Column(name = "registered_area_ids", columnDefinition = "bigint[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<Long> registeredAreaIds;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING; // Trạng thái duyệt hồ sơ

    @Column(name = "avg_rating", precision = 3, scale = 2)
    private java.math.BigDecimal avgRating = new java.math.BigDecimal("5.00"); // Điểm đánh giá trung bình

    @Column(name = "total_deliveries")
    private Integer totalDeliveries = 0; // Tổng số đơn hàng đã giao thành công

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason; // Lý do từ chối hồ sơ nếu có

    @Column(name = "approved_by")
    private Long approvedBy; // ID của Admin đã duyệt hồ sơ

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt; // Thời điểm được duyệt hồ sơ

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt; // Thời điểm tạo bản ghi

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt; // Thời điểm cập nhật bản ghi gần nhất
}