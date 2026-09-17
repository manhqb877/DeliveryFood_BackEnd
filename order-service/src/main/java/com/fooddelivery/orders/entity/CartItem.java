package com.fooddelivery.orders.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "cart_items", indexes = {
        @Index(name = "idx_cart_items_cart", columnList = "cart_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart; // Giỏ hàng chứa món này

    @Column(name = "item_id", nullable = false)
    private Long itemId; // Tham chiếu lỏng tới core_db.items.id

    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName; // Snapshot tên món tại thời điểm thêm

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice; // Snapshot giá đơn vị

    @Column(nullable = false)
    private Short quantity = 1; // Số lượng món

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "selected_options", columnDefinition = "jsonb")
    private List<Map<String, Object>> selectedOptions; // Các tùy chọn/topping đã chọn (JSONB)

    @Column(name = "item_note", columnDefinition = "TEXT")
    private String itemNote; // Ghi chú riêng cho món

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice; // Tổng tiền của dòng món này (unitPrice * quantity + extra options)
}