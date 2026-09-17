package com.fooddelivery.core.entity;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "item_options", indexes = {
        @Index(name = "idx_item_options_item", columnList = "item_id, is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item; // Món ăn sở hữu tùy chọn này

    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName; // Tên nhóm tùy chọn (VD: "Kích cỡ", "Topping", "Mức đường")

    @Column(name = "option_name", nullable = false, length = 100)
    private String optionName; // Tên lựa chọn cụ thể (VD: "Size L", "Thêm trứng")

    @Column(name = "extra_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal extraPrice = BigDecimal.ZERO; // Giá cộng thêm (0 = miễn phí)

    @Column(name = "is_required")
    private Boolean isRequired = false; // TRUE = bắt buộc chọn 1 trong nhóm

    @Column(name = "is_multiple")
    private Boolean isMultiple = false; // TRUE = cho phép chọn nhiều (checkbox)

    @Column(name = "max_select")
    private Short maxSelect = 1; // Số lượng tối đa được chọn trong nhóm

    @Column(name = "is_active")
    private Boolean isActive = true; // Trạng thái hoạt động

    @Column(name = "sort_order")
    private Short sortOrder = 0; // Thứ tự sắp xếp hiển thị
}