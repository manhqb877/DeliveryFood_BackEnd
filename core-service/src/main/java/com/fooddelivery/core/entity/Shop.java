package com.fooddelivery.core.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "shops", indexes = {
        @Index(name = "idx_shops_area", columnList = "area_id, is_active, is_open"),
        @Index(name = "idx_shops_rating", columnList = "avg_rating DESC")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shop {

    @Id
    private Long id; // Khớp với identity_db.shop_profiles.id (không auto-increment vì đồng bộ qua Kafka)

    @Column(name = "area_id", nullable = false)
    private Long areaId; // ID khu vực đặt gian hàng

    @Column(name = "owner_id", nullable = false)
    private Long ownerId; // ID chủ gian hàng

    @Column(name = "shop_name", nullable = false, length = 255)
    private String shopName; // Tên gian hàng

    @Column(name = "shop_description", columnDefinition = "TEXT")
    private String shopDescription; // Mô tả gian hàng

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl; // URL logo gian hàng

    @Column(name = "cover_image_url", columnDefinition = "TEXT")
    private String coverImageUrl; // URL ảnh bìa gian hàng

    @Column(length = 15)
    private String phone; // Số điện thoại liên hệ

    @Column(name = "shop_lat")
    private Double shopLat; // Vĩ độ (copy từ shop_profiles)

    @Column(name = "shop_lng")
    private Double shopLng; // Kinh độ

    @Column(name = "location_detail", length = 255)
    private String locationDetail; // Chi tiết vị trí nội khu

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "business_hours", columnDefinition = "jsonb")
    private List<Map<String, Object>> businessHours; // Thời gian hoạt động theo tuần (JSONB)

    @Column(name = "is_open")
    private Boolean isOpen = false; // Trạng thái shop tự bật/tắt nhận đơn

    @Column(name = "is_accepting_orders")
    private Boolean isAcceptingOrders = true; // Trạng thái hệ thống cho phép nhận đơn

    @Column(name = "avg_rating", precision = 3, scale = 2)
    private BigDecimal avgRating = BigDecimal.ZERO; // Điểm đánh giá trung bình

    @Column(name = "total_reviews")
    private Integer totalReviews = 0; // Tổng số lượt đánh giá

    @Column(name = "avg_prep_time_minutes")
    private Short avgPrepTimeMinutes = 15; // Thời gian chuẩn bị trung bình (phút)

    @Column(name = "is_active")
    private Boolean isActive = true; // Trạng thái hoạt động

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt; // Thời điểm tạo bản ghi

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt; // Thời điểm cập nhật bản ghi gần nhất
}