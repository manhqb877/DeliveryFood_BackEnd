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
public class ShopDetailsResponse {
    private Long id;
    private String shopName;
    private String locationDetail;
    private Double shopLat;
    private Double shopLng;
    private BigDecimal avgRating;
    private Integer totalReviews;
    private List<CategoryDto> categories;
}
