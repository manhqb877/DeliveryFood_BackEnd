package com.fooddelivery.core.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemOptionDto {
    private Long id;
    private String groupName;
    private String optionName;
    private BigDecimal extraPrice;
    private Boolean isRequired;
    private Boolean isMultiple;
    private Short maxSelect;
}
