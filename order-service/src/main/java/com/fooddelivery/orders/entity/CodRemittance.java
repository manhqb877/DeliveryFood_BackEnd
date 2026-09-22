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

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
}
