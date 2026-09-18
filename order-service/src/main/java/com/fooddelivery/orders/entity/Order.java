package com.fooddelivery.orders.entity;


import com.fooddelivery.orders.enums.OrderStatus;
import com.fooddelivery.orders.enums.PaymentMethod;
import com.fooddelivery.orders.enums.PaymentStatus;
import com.fooddelivery.orders.enums.SagaStatus;
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
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_user", columnList = "user_id"),
        @Index(name = "idx_orders_guest", columnList = "guest_session_id"),
        @Index(name = "idx_orders_shop", columnList = "shop_id, order_status"),
        @Index(name = "idx_orders_code", columnList = "order_code"),
        @Index(name = "idx_orders_status", columnList = "order_status, placed_at DESC")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @Column(name = "order_code", nullable = false, unique = true, length = 20)
    private String orderCode; // Mã ngắn tra cứu (VD: 'ORD-20260917-ABCD')

    @Column(name = "user_id")
    private Long userId; // ID Khách hàng (NULL nếu Guest)

    @Column(name = "guest_session_id")
    private Long guestSessionId; // ID phiên vãng lai

    @Column(name = "shop_id", nullable = false)
    private Long shopId; // ID gian hàng

    @Column(name = "shop_name", nullable = false, length = 255)
    private String shopName; // Snapshot tên quán

    @Column(name = "area_id", nullable = false)
    private Long areaId; // ID khu vực

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "delivery_address", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> deliveryAddress; // Snapshot địa chỉ giao hàng bất biến (JSONB)

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal; // Tiền món tạm tính

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO; // Tiền giảm giá

    @Column(name = "delivery_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal deliveryFee = BigDecimal.ZERO; // Phí giao hàng

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount; // Tổng tiền thanh toán cuối cùng

    @Column(name = "promotion_id")
    private Long promotionId; // ID khuyến mãi đã dùng

    @Column(name = "promotion_code", length = 50)
    private String promotionCode; // Mã khuyến mãi

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod; // Phương thức thanh toán (COD, ONLINE, WALLET)

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING; // Trạng thái thanh toán

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 30)
    private OrderStatus orderStatus = OrderStatus.PLACED; // Trạng thái đơn hàng FSM

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason; // Lý do hủy đơn

    @Column(name = "cancelled_by", length = 20)
    private String cancelledBy; // Tác nhân hủy đơn

    @Column(name = "order_note", columnDefinition = "TEXT")
    private String orderNote; // Ghi chú đơn hàng chung

    @Column(name = "idempotency_key", unique = true, length = 64)
    private String idempotencyKey; // Chống tạo đơn trùng lặp

    // Mốc thời gian SLA
    @Column(name = "placed_at", nullable = false)
    private OffsetDateTime placedAt = OffsetDateTime.now();

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    @Column(name = "ready_at")
    private OffsetDateTime readyAt;

    @Column(name = "picked_up_at")
    private OffsetDateTime pickedUpAt;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "saga_status", length = 20)
    private SagaStatus sagaStatus = SagaStatus.IN_PROGRESS; // Trạng thái Saga Orchestrator

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}