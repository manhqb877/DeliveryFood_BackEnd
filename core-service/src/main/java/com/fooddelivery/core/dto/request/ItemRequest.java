package com.fooddelivery.core.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemRequest {
    @NotNull(message = "Shop ID is required")
    private Long shopId;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotBlank(message = "Item name is required")
    private String name;

    private String description;

    private String imageUrl;

    private List<String> extraImageUrls;

    @NotNull(message = "Base price is required")
    private BigDecimal basePrice;
    private BigDecimal discountPrice;

    private String status; // "AVAILABLE", "SOLD_OUT", "HIDDEN", "DISCONTINUED"

    private Integer dailyLimit;

    private Short prepTimeMinutes;

    private List<String> tags;

    private Short sortOrder;
}
