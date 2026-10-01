package com.fooddelivery.core.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedeemPromotionRequest {
    private Long promotionId;
    private String promoCode;
    private Long orderId;
    private Long userId;
    private Long guestSessionId;
    private BigDecimal orderAmount;
    private BigDecimal discountAmount;
    private String idempotencyKey;
}
