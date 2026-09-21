package com.fooddelivery.core.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkItemOptionUpdateRequest {
    @NotBlank(message = "Old group name is required")
    private String oldGroupName;

    @NotBlank(message = "Old option name is required")
    private String oldOptionName;

    @NotNull(message = "New option data is required")
    @Valid
    private ItemOptionRequest newOption;
}
