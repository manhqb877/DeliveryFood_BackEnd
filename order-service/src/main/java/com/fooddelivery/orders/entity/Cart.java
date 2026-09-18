package com.fooddelivery.orders.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "carts", uniqueConstraints = {
        @UniqueConstraint(name = "uq_cart_user_shop", columnNames = {"user_id", "shop_id"}),
        @UniqueConstraint(name = "uq_cart_guest_shop", columnNames = {"guest_session_id", "shop_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @Column(name = "user_id")
    private Long userId; // ID Khách hàng (NULL nếu là Guest)

    @Column(name = "guest_session_id")
    private Long guestSessionId; // ID phiên vãng lai (NULL nếu là Khách hàng)

    @Column(name = "shop_id", nullable = false)
    private Long shopId; // ID gian hàng

    @Column(name = "area_id", nullable = false)
    private Long areaId; // ID khu vực

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO; // Tổng tiền tạm tính

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO; // Số tiền được giảm

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "delivery_address", columnDefinition = "jsonb")
    private Map<String, Object> deliveryAddress; // Địa chỉ nhận hàng tạm tính (JSONB)

    @Column(name = "promotion_code", length = 50)
    private String promotionCode; // Mã khuyến mãi đang áp dụng trong giỏ

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt; // Thời gian hết hạn giỏ hàng

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}