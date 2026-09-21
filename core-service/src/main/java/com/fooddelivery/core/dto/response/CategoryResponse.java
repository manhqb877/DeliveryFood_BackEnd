package com.fooddelivery.core.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private Long id;

    @JsonProperty("shop_id")
    private Long shopId;

    private String name;
    private String description;

    @JsonProperty("image_url")
    private String imageUrl;

    @JsonProperty("icon_emoji")
    private String iconEmoji;

    @JsonProperty("sort_order")
    private Short sortOrder;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("available_from")
    private LocalTime availableFrom;

    @JsonProperty("available_until")
    private LocalTime availableUntil;
}
