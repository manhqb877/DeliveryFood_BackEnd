package com.fooddelivery.orders.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "cod_remittances", schema = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodRemittance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "order_code", length = 100)
    private String orderCode;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(name = "shipper_id", nullable = false)
    private Long shipperId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // PENDING, COMPLETED

    @Column(name = "is_multi_shop")
    private Boolean isMultiShop;

    @Column(name = "shop2_id")
    private Long shop2Id;

    @Column(name = "shop1_name")
    private String shop1Name;

    @Column(name = "shop2_name")
    private String shop2Name;

    @Column(name = "shop1_amount")
    private BigDecimal shop1Amount;

    @Column(name = "shop2_amount")
    private BigDecimal shop2Amount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
}
