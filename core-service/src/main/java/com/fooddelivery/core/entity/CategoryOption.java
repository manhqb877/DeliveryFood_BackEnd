package com.fooddelivery.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "category_options", indexes = {
        @Index(name = "idx_category_options_cat", columnList = "category_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;

    @Column(name = "option_name", nullable = false, length = 100)
    private String optionName;

    @Column(name = "extra_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal extraPrice;

    @Column(name = "is_required")
    private Boolean isRequired = false;

    @Column(name = "is_multiple")
    private Boolean isMultiple = false;

    @Column(name = "max_select")
    private Short maxSelect = 1;

    @Column(name = "sort_order")
    private Short sortOrder = 0;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
}
