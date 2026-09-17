package com.fooddelivery.tracking.entity;


import com.fooddelivery.tracking.enums.ShiftStatus;
import com.fooddelivery.tracking.enums.ShiftType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "shipper_shifts", indexes = {
        @Index(name = "idx_shifts_shipper", columnList = "shipper_id, status"),
        @Index(name = "idx_shifts_area", columnList = "area_id, planned_start")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipperShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @Column(name = "shipper_id", nullable = false)
    private Long shipperId;

    @Column(name = "area_id", nullable = false)
    private Long areaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type", length = 20)
    private ShiftType shiftType;

    @Column(name = "planned_start", nullable = false)
    private OffsetDateTime plannedStart;

    @Column(name = "planned_end", nullable = false)
    private OffsetDateTime plannedEnd;

    @Column(name = "actual_start")
    private OffsetDateTime actualStart;

    @Column(name = "actual_end")
    private OffsetDateTime actualEnd;

    @Column(name = "total_orders")
    private Short totalOrders = 0;

    @Column(name = "total_distance_m")
    private Integer totalDistanceM = 0;

    @Column(name = "total_earnings", precision = 12, scale = 2)
    private BigDecimal totalEarnings = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShiftStatus status = ShiftStatus.SCHEDULED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}