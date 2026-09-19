package com.fooddelivery.core.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemDto {
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private BigDecimal basePrice;
    private String status;
    private BigDecimal avgRating;
    private Integer totalReviews;
    private List<ItemOptionDto> options;
    private BigDecimal originalPrice;
    private String priceName;
    private Long shopId;
    private String shopName;
}
