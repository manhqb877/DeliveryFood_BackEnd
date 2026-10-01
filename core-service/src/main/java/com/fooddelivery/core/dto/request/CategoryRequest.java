package com.fooddelivery.core.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Data
public class CategoryRequest {
    @NotNull(message = "Shop ID must not be null")
    @JsonAlias({"shop_id", "shopId"})
    private Long shopId;

    @NotBlank(message = "Name must not be blank")
    private String name;

    private String description;

    @JsonAlias({"image_url", "imageUrl"})
    private String imageUrl;

    @JsonAlias({"icon_emoji", "iconEmoji"})
    private String iconEmoji;

    @JsonAlias({"sort_order", "sortOrder"})
    private Short sortOrder;

    @JsonAlias({"is_active", "isActive"})
    private Boolean isActive;

    @JsonAlias({"available_from", "availableFrom"})
    private LocalTime availableFrom;

    @JsonAlias({"available_until", "availableUntil"})
    private LocalTime availableUntil;
}
