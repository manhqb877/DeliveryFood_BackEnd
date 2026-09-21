package com.fooddelivery.core.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemOptionRequest {
    @NotBlank(message = "Group name is required")
    private String groupName;

    @NotBlank(message = "Option name is required")
    private String optionName;

    private BigDecimal extraPrice;

    private Boolean isRequired;

    private Boolean isMultiple;

    private Short maxSelect;
    
    private Short sortOrder;
}
