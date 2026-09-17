package com.fooddelivery.core.entity;


import com.fooddelivery.core.enums.PriceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "item_prices", indexes = {
        @Index(name = "idx_item_prices_item", columnList = "item_id, is_active"),
        @Index(name = "idx_item_prices_type", columnList = "price_type, is_active"),
        @Index(name = "idx_item_prices_priority", columnList = "item_id, priority DESC")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item; // Món ăn áp dụng bảng giá này

    @Column(name = "price_name", nullable = false, length = 100)
    private String priceName; // Tên mô tả (VD: "Giá giờ vàng", "Giá Tết")

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price; // Mức giá áp dụng thực tế

    @Enumerated(EnumType.STRING)
    @Column(name = "price_type", nullable = false, length = 30)
    private PriceType priceType; // Phân loại giá (NORMAL, PEAK_HOUR, SEASONAL, v.v.)


    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "applicable_days", columnDefinition = "smallint[]")
    private Short[] applicableDays;

    @Column(name = "time_start")
    private LocalTime timeStart; // Giờ bắt đầu hiệu lực trong ngày (VD: 11:00)

    @Column(name = "time_end")
    private LocalTime timeEnd; // Giờ kết thúc hiệu lực trong ngày (VD: 13:30)

    @Column(name = "valid_from")
    private LocalDate validFrom; // Ngày bắt đầu hiệu lực theo khoảng thời gian cụ thể

    @Column(name = "valid_until")
    private LocalDate validUntil; // Ngày kết thúc hiệu lực

    @Column(nullable = false)
    private Short priority = 0; // Độ ưu tiên (số càng cao ưu tiên càng lớn)

    @Column(name = "is_active")
    private Boolean isActive = true; // Trạng thái hiệu lực

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt; // Thời điểm tạo bản ghi

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt; // Thời điểm cập nhật bản ghi gần nhất
}