package com.fooddelivery.core.service;

import com.fooddelivery.core.dto.request.CreatePromotionRequest;
import com.fooddelivery.core.dto.request.ValidatePromotionRequest;
import com.fooddelivery.core.dto.response.PromotionRedemptionResponse;
import com.fooddelivery.core.dto.response.PromotionResponse;
import com.fooddelivery.core.dto.response.PromotionValidationResponse;

import java.math.BigDecimal;
import java.util.List;

public interface PromotionService {

    List<PromotionResponse> getShopPromotions(Long shopId);

    PromotionResponse createShopPromotion(Long shopId, CreatePromotionRequest request);

    PromotionResponse togglePromotionStatus(Long promotionId, Boolean isActive);

    PromotionValidationResponse validatePromotion(ValidatePromotionRequest request);

    PromotionRedemptionResponse redeemPromotion(Long promotionId, Long orderId, Long userId, Long guestSessionId, BigDecimal orderAmount);

    List<PromotionRedemptionResponse> getPromotionRedemptions(Long promotionId);

    List<PromotionResponse> getAllPromotionsForAdmin(com.fooddelivery.core.enums.PromoScope scope, String approvalStatus);

    PromotionResponse createPlatformPromotion(CreatePromotionRequest request);

    PromotionResponse updatePromotion(Long id, CreatePromotionRequest request);

    PromotionResponse approveOrRejectPromotion(Long id, Boolean approved, String rejectionReason);
}
