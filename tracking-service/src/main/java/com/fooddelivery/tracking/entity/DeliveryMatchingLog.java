package com.fooddelivery.tracking.entity;


import com.fooddelivery.tracking.enums.MatchResponse;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "delivery_matching_logs", indexes = {
        @Index(name = "idx_matching_delivery", columnList = "delivery_id, offered_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryMatchingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id", nullable = false)
    private Delivery delivery;

    @Column(name = "shipper_id", nullable = false)
    private Long shipperId;

    @Column(name = "match_score", precision = 5, scale = 2)
    private BigDecimal matchScore;

    @Column(name = "distance_m")
    private Integer distanceM;

    @Column(name = "shipper_rating", precision = 3, scale = 2)
    private BigDecimal shipperRating;

    @CreationTimestamp
    @Column(name = "offered_at", nullable = false, updatable = false)
    private OffsetDateTime offeredAt;

    @Column(name = "response_at")
    private OffsetDateTime responseAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MatchResponse response; // ACCEPTED, REJECTED, TIMEOUT

    @Column(name = "rejection_reason", length = 100)
    private String rejectionReason;
}