package com.fooddelivery.orders.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "reviews", indexes = {
        @Index(name = "idx_reviews_shop", columnList = "shop_id, created_at DESC"),
        @Index(name = "idx_reviews_order", columnList = "order_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId; // ID đơn hàng được đánh giá (1 đơn = 1 đánh giá duy nhất)

    @Column(name = "user_id")
    private Long userId; // ID Khách hàng đánh giá

    @Column(name = "guest_session_id")
    private Long guestSessionId; // ID Guest đánh giá

    @Column(name = "shop_id", nullable = false)
    private Long shopId; // ID gian hàng được đánh giá

    @Column(name = "shop_rating", nullable = false)
    private Short shopRating; // Điểm đánh giá quán (1 đến 5)

    @Column(name = "shop_comment", columnDefinition = "TEXT")
    private String shopComment; // Nội dung đánh giá quán

    @Column(name = "delivery_id")
    private Long deliveryId; // ID chuyến giao hàng (từ tracking-service)

    @Column(name = "shipper_id")
    private Long shipperId; // ID Shipper được đánh giá

    @Column(name = "shipper_rating")
    private Short shipperRating; // Điểm đánh giá shipper (1 đến 5)

    @Column(name = "shipper_comment", columnDefinition = "TEXT")
    private String shipperComment; // Nội dung đánh giá shipper

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "image_urls", columnDefinition = "text[]")
    private String[] imageUrls; // Mảng URL ảnh đính kèm

    @Column(name = "shop_reply", columnDefinition = "TEXT")
    private String shopReply; // Phản hồi từ Shop Manager

    @Column(name = "shop_replied_at")
    private OffsetDateTime shopRepliedAt; // Thời điểm shop phản hồi

    @Column(name = "is_anonymous")
    private Boolean isAnonymous = false; // Đánh giá ẩn danh

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}