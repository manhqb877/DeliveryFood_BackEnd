package com.fooddelivery.payment.entity;


import com.fooddelivery.payment.enums.CommissionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "commission_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @Column(name = "shop_id")
    private Long shopId; // ID gian hàng (NULL nếu cấu hình áp dụng chung cho cả khu vực)

    @Column(name = "area_id")
    private Long areaId; // ID khu vực (NULL nếu cấu hình áp dụng cho riêng gian hàng cụ thể)

    @Enumerated(EnumType.STRING)
    @Column(name = "commission_type", nullable = false, length = 20)
    private CommissionType commissionType = CommissionType.PERCENT; // PERCENT hoặc FIXED_PER_ORDER

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal rate; // Tỷ lệ % hoa hồng hoặc số tiền cố định mỗi đơn

    @Column(name = "valid_from", nullable = false)
    private OffsetDateTime validFrom = OffsetDateTime.now(); // Thời điểm bắt đầu hiệu lực

    @Column(name = "valid_until")
    private OffsetDateTime validUntil; // Thời điểm hết hạn

    @Column(name = "created_by")
    private Long createdBy; // ID Admin tạo cấu hình

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}