package com.fooddelivery.orders.entity;


import com.fooddelivery.orders.enums.ComplaintReason;
import com.fooddelivery.orders.enums.ComplaintStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "complaints")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @Column(name = "order_id", nullable = false)
    private Long orderId; // ID đơn hàng phát sinh khiếu nại

    @Column(name = "complainant_user_id")
    private Long complainantUserId; // ID người dùng khiếu nại

    @Column(name = "complainant_guest_id")
    private Long complainantGuestId; // ID guest khiếu nại

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_type", nullable = false, length = 50)
    private ComplaintReason reasonType; // Loại khiếu nại (WRONG_ITEM, MISSING_ITEM, v.v.)

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description; // Mô tả chi tiết khiếu nại

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "image_urls", columnDefinition = "text[]")
    private String[] imageUrls; // Mảng URL ảnh bằng chứng khiếu nại

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComplaintStatus status = ComplaintStatus.OPEN; // Trạng thái (OPEN, IN_REVIEW, RESOLVED, CLOSED)

    @Column(columnDefinition = "TEXT")
    private String resolution; // Hướng giải quyết / kết quả xử lý

    @Column(name = "resolved_by")
    private Long resolvedBy; // ID Admin giải quyết khiếu nại

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt; // Thời điểm giải quyết xong

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}