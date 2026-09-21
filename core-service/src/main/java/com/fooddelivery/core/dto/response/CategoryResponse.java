package com.fooddelivery.core.dto.response;

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

    private Long shopId;

    private String name;
    private String description;

    private String imageUrl;

    private String iconEmoji;

    private Short sortOrder;

    private Boolean isActive;

    private LocalTime availableFrom;

    private LocalTime availableUntil;
}
