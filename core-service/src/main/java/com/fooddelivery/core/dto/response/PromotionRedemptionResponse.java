package com.fooddelivery.core.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionRedemptionResponse {
    private Long id;
    private Long promotionId;
    private String promoCode;
    private Long userId;
    private Long guestSessionId;
    private Long orderId;
    private BigDecimal discountApplied;
    private String status;
    private OffsetDateTime createdAt;
}
