package com.fooddelivery.core.entity;


import com.fooddelivery.core.enums.ItemStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "items", indexes = {
        @Index(name = "idx_items_shop", columnList = "shop_id, status"),
        @Index(name = "idx_items_category", columnList = "category_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop; // Gian hàng sở hữu món ăn

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category; // Danh mục chứa món ăn

    @Column(nullable = false, length = 255)
    private String name; // Tên món ăn

    @Column(columnDefinition = "TEXT")
    private String description; // Mô tả món ăn

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl; // URL hình ảnh món ăn

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice; // Giá gốc cơ bản (fallback khi không có rule giá theo thời điểm)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemStatus status = ItemStatus.AVAILABLE; // Trạng thái món (AVAILABLE, SOLD_OUT, HIDDEN, DISCONTINUED)

    @Column(name = "daily_limit")
    private Integer dailyLimit; // Giới hạn số lượng bán trong ngày (NULL = không giới hạn)

    @Column(name = "daily_sold")
    private Integer dailySold = 0; // Số lượng đã bán trong ngày (Reset lúc 00:00)

    @Column(name = "prep_time_minutes")
    private Short prepTimeMinutes = 10; // Thời gian chuẩn bị món (phút)

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", columnDefinition = "varchar[]")
    private String[] tags;

    @Column(name = "avg_rating", precision = 3, scale = 2)
    private BigDecimal avgRating = BigDecimal.ZERO; // Điểm đánh giá trung bình món

    @Column(name = "total_reviews")
    private Integer totalReviews = 0; // Tổng số lượt đánh giá món

    @Column(name = "sort_order")
    private Short sortOrder = 0; // Thứ tự sắp xếp

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt; // Thời điểm tạo bản ghi

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt; // Thời điểm cập nhật bản ghi gần nhất
}