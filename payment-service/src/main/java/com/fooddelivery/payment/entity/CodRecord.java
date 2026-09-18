package com.fooddelivery.payment.entity;


import com.fooddelivery.payment.enums.ReconcileStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "cod_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @Column(name = "delivery_id", nullable = false, unique = true)
    private Long deliveryId; // Tham chiếu tới transaction / delivery tương ứng

    @Column(name = "shipper_id", nullable = false)
    private Long shipperId; // ID Shipper thu tiền mặt

    @Column(name = "shop_id", nullable = false)
    private Long shopId; // ID gian hàng nhận tiền đối soát

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount; // Số tiền COD đã thu

    @Column(name = "collected_at", nullable = false)
    private OffsetDateTime collectedAt; // Thời điểm shipper thu tiền

    @Enumerated(EnumType.STRING)
    @Column(name = "reconcile_status", length = 20)
    private ReconcileStatus reconcileStatus = ReconcileStatus.PENDING; // Trạng thái đối soát (PENDING, CONFIRMED, DISPUTED)

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}