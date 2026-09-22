package com.fooddelivery.orders.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "commission_configs", schema = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_id")
    private Long shopId;

    @Column(name = "shop_name")
    private String shopName;

    @Column(name = "area_id")
    private Long areaId;

    @Column(name = "area_name")
    private String areaName;

    @Column(name = "commission_type", nullable = false)
    private String commissionType;

    @Column(name = "rate", nullable = false)
    private Double rate;

    @CreationTimestamp
    @Column(name = "valid_from", updatable = false)
    private OffsetDateTime validFrom;
}
