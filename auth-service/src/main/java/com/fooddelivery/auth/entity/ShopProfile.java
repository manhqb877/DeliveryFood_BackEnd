package com.fooddelivery.auth.entity;

import com.fooddelivery.auth.enums.ApprovalStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "shop_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner; // Chủ gian hàng (User có role SHOP_MANAGER)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area; // Khu vực đặt gian hàng

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_node_id")
    private IntraZoneMap locationNode; // Nút định tuyến nội bộ nơi gian hàng đặt

    @Column(name = "location_detail", length = 255)
    private String locationDetail; // Chi tiết vị trí (VD: "Tầng 1 Tòa A, cạnh thang máy")

    @Column(name = "shop_lat")
    private Double shopLat; // Vĩ độ chính xác của gian hàng (cho shipper điều hướng)

    @Column(name = "shop_lng")
    private Double shopLng; // Kinh độ chính xác của gian hàng

    @Column(name = "shop_name", nullable = false, length = 255)
    private String shopName; // Tên gian hàng

    @Column(name = "shop_description", columnDefinition = "TEXT")
    private String shopDescription; // Mô tả gian hàng

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl; // URL logo gian hàng

    @Column(name = "cover_image_url", columnDefinition = "TEXT")
    private String coverImageUrl; // URL ảnh bìa gian hàng

    @Column(length = 15)
    private String phone; // Số điện thoại liên hệ tại quán

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "business_hours", columnDefinition = "jsonb")
    private List<Map<String, Object>> businessHours; // Thời gian hoạt động theo ngày trong tuần (JSONB)

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING; // Trạng thái phê duyệt hồ sơ

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason; // Lý do từ chối nếu hồ sơ không đạt

    @Column(name = "is_open")
    private Boolean isOpen = false; // Trạng thái bật/tắt nhận đơn do shop tự điều chỉnh

    @Column(name = "is_accepting_orders")
    private Boolean isAcceptingOrders = true; // Trạng thái hệ thống cho phép nhận đơn

    @Column(name = "commission_rate", precision = 5, scale = 2)
    private java.math.BigDecimal commissionRate; // Tỷ lệ hoa hồng riêng (NULL = dùng mặc định của khu)

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Map<String, Object>> documents; // Danh sách URL giấy tờ pháp lý, ảnh quán (JSONB)

    @Column(name = "approved_by")
    private Long approvedBy; // ID của Admin đã duyệt hồ sơ

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt; // Thời điểm hồ sơ được duyệt

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt; // Thời điểm tạo bản ghi

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt; // Thời điểm cập nhật bản ghi gần nhất
}