package com.fooddelivery.orders.entity;


import com.fooddelivery.orders.enums.OutboxStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "outbox_events", indexes = {
        @Index(name = "idx_outbox_pending", columnList = "status, created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType; // Phân loại aggregate (VD: 'ORDER', 'PAYMENT')

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId; // ID của aggregate

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType; // Loại sự kiện (VD: 'order.placed')

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload; // Dữ liệu sự kiện JSONB

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status = OutboxStatus.PENDING; // Trạng thái (PENDING, PUBLISHED, FAILED)

    @Column(name = "retry_count")
    private Short retryCount = 0; // Số lần thử lại publish

    @Column(name = "published_at")
    private OffsetDateTime publishedAt; // Thời điểm publish thành công

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}