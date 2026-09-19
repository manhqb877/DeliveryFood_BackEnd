package com.fooddelivery.auth.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
public class UserAddressDto {
    private Long id;
    private String addressLine;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Boolean isDefault;
    private OffsetDateTime createdAt;
}
