package com.fooddelivery.core.entity;

import com.fooddelivery.core.enums.RedemptionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "promotion_redemptions", indexes = {
        @Index(name = "idx_promo_order", columnList = "order_id"),
        @Index(name = "idx_promo_user_unique", columnList = "promotion_id, user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion; // Chương trình khuyến mãi được áp dụng

    @Column(name = "user_id")
    private Long userId; // ID Khách hàng (NULL nếu là Guest)

    @Column(name = "guest_session_id")
    private Long guestSessionId; // ID phiên vãng lai (NULL nếu là Khách hàng đã đăng ký)

    @Column(name = "order_id")
    private Long orderId; // ID đơn hàng thực tế áp dụng mã này

    @Column(name = "discount_applied", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountApplied; // Số tiền thực tế được trừ vào đơn hàng

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RedemptionStatus status = RedemptionStatus.USED; // Trạng thái sử dụng (LOCKED, USED, RELEASED)

    @Column(name = "idempotency_key", unique = true, length = 64)
    private String idempotencyKey; // Khóa chống redeem trùng lặp

    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil; // Thời hạn giữ chỗ (lock) mã khuyến mãi

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt; // Thời điểm tạo giao dịch đổi mã
}