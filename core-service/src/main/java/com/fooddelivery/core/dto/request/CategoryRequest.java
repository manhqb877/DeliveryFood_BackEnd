package com.fooddelivery.core.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Data
public class CategoryRequest {
    @NotNull(message = "Shop ID must not be null")
    private Long shopId;

    @NotBlank(message = "Name must not be blank")
    private String name;

    private String description;

    private String imageUrl;

    private String iconEmoji;

    private Short sortOrder;

    private Boolean isActive;

    private LocalTime availableFrom;

    private LocalTime availableUntil;
}
