package com.fooddelivery.orders.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "order_items", indexes = {
        @Index(name = "idx_order_items_order", columnList = "order_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order; // Đơn hàng chứa món

    @Column(name = "item_id", nullable = false)
    private Long itemId; // Tham chiếu lỏng core_db.items.id

    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName; // Snapshot tên món bất biến

    @Column(name = "item_image_url", columnDefinition = "TEXT")
    private String itemImageUrl; // Snapshot hình ảnh món

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice; // Snapshot giá món thực tế

    @Column(nullable = false)
    private Short quantity; // Số lượng

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "selected_options", columnDefinition = "jsonb")
    private List<Map<String, Object>> selectedOptions; // Snapshot tùy chọn đã chọn (JSONB)

    @Column(name = "item_note", columnDefinition = "TEXT")
    private String itemNote; // Ghi chú món

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice; // Tổng tiền món
}