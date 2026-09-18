package com.fooddelivery.core.entity;


import com.fooddelivery.core.enums.ApplicableTo;
import com.fooddelivery.core.enums.PromoScope;
import com.fooddelivery.core.enums.PromoType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "promotions", indexes = {
        @Index(name = "idx_promotions_code", columnList = "code"),
        @Index(name = "idx_promotions_active", columnList = "scope, is_active, valid_until")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @Column(nullable = false, unique = true, length = 50)
    private String code; // Mã khuyến mãi duy nhất (VD: 'CHUNGCU10')

    @Enumerated(EnumType.STRING)
    @Column(name = "promo_type", nullable = false, length = 20)
    private PromoType promoType; // Loại khuyến mãi (PERCENT, FIXED_AMOUNT, FREE_DELIVERY, FREE_ITEM)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PromoScope scope; // Phạm vi tạo (PLATFORM, SHOP, AREA)

    @Column(name = "shop_id")
    private Long shopId; // ID gian hàng (NULL nếu scope = PLATFORM)

    @Column(name = "area_id")
    private Long areaId; // ID khu vực (NULL nếu scope != AREA)

    @Column(name = "discount_value", precision = 12, scale = 2)
    private BigDecimal discountValue; // Giá trị giảm (số tiền hoặc phần trăm)

    @Column(name = "min_order_value", precision = 12, scale = 2)
    private BigDecimal minOrderValue = BigDecimal.ZERO; // Giá trị đơn hàng tối thiểu để áp dụng

    @Column(name = "max_discount_amount", precision = 12, scale = 2)
    private BigDecimal maxDiscountAmount; // Giới hạn số tiền giảm tối đa khi dùng PERCENT

    @Column(name = "total_limit")
    private Integer totalLimit; // Tổng số lượt sử dụng tối đa trên toàn hệ thống

    @Column(name = "per_user_limit")
    private Short perUserLimit = 1; // Số lần tối đa 1 user / guest được dùng mã này

    @Column(name = "used_count")
    private Integer usedCount = 0; // Đếm số lượt đã sử dụng thực tế

    @Enumerated(EnumType.STRING)
    @Column(name = "applicable_to", length = 20)
    private ApplicableTo applicableTo = ApplicableTo.ALL; // Đối tượng áp dụng (ALL, CUSTOMER, RESIDENT, NEW_USER)

    @Column(name = "valid_from", nullable = false)
    private OffsetDateTime validFrom; // Thời điểm bắt đầu hiệu lực khuyến mãi

    @Column(name = "valid_until", nullable = false)
    private OffsetDateTime validUntil; // Thời điểm hết hạn khuyến mãi

    @Column(name = "approval_status", length = 20)
    private String approvalStatus = "APPROVED"; // Trạng thái phê duyệt (PENDING, APPROVED, REJECTED)

    @Column(name = "is_active")
    private Boolean isActive = true; // Trạng thái hoạt động

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt; // Thời điểm tạo bản ghi

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt; // Thời điểm cập nhật bản ghi gần nhất
}