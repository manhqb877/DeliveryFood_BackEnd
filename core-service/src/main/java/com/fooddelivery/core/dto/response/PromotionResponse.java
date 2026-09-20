package com.fooddelivery.core.dto.response;

import com.fooddelivery.core.enums.ApplicableTo;
import com.fooddelivery.core.enums.PromoScope;
import com.fooddelivery.core.enums.PromoType;
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
public class PromotionResponse {
    private Long id;
    private String code;
    private PromoType promoType;
    private PromoScope scope;
    private Long shopId;
    private Long areaId;
    private BigDecimal discountValue;
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscountAmount;
    private Integer totalLimit;
    private Short perUserLimit;
    private Integer usedCount;
    private Integer remainingQuota;
    private ApplicableTo applicableTo;
    private OffsetDateTime validFrom;
    private OffsetDateTime validUntil;
    private String approvalStatus;
    private Boolean isActive;
    private OffsetDateTime createdAt;
}
