package com.fooddelivery.core.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Data
public class CategoryRequest {
    @NotNull(message = "Shop ID must not be null")
    @JsonProperty("shop_id")
    private Long shopId;

    @NotBlank(message = "Name must not be blank")
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
