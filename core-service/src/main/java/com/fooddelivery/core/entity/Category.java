package com.fooddelivery.core.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "categories", indexes = {
        @Index(name = "idx_categories_shop", columnList = "shop_id, is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop; // Gian hàng sở hữu danh mục này

    @Column(nullable = false, length = 100)
    private String name; // Tên danh mục (VD: "Cơm phần", "Nước uống")

    @Column(columnDefinition = "TEXT")
    private String description; // Mô tả danh mục

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl; // URL hình ảnh danh mục

    @Column(name = "sort_order")
    private Short sortOrder = 0; // Thứ tự sắp xếp hiển thị

    @Column(name = "is_active")
    private Boolean isActive = true; // Trạng thái hoạt động

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt; // Thời điểm tạo bản ghi
}