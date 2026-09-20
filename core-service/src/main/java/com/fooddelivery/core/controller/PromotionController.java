package com.fooddelivery.core.controller;

import com.fooddelivery.core.dto.request.CreatePromotionRequest;
import com.fooddelivery.core.dto.request.ValidatePromotionRequest;
import com.fooddelivery.core.dto.response.PromotionRedemptionResponse;
import com.fooddelivery.core.dto.response.PromotionResponse;
import com.fooddelivery.core.dto.response.PromotionValidationResponse;
import com.fooddelivery.core.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping("/shop/{shopId}")
    public ResponseEntity<List<PromotionResponse>> getShopPromotions(@PathVariable Long shopId) {
        List<PromotionResponse> responses = promotionService.getShopPromotions(shopId);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/shop/{shopId}")
    public ResponseEntity<PromotionResponse> createShopPromotion(
            @PathVariable Long shopId,
            @Valid @RequestBody CreatePromotionRequest request
    ) {
        PromotionResponse response = promotionService.createShopPromotion(shopId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<PromotionResponse> togglePromotion(
            @PathVariable Long id,
            @RequestParam Boolean isActive
    ) {
        PromotionResponse response = promotionService.togglePromotionStatus(id, isActive);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromotionResponse> updatePromotion(
            @PathVariable Long id,
            @Valid @RequestBody CreatePromotionRequest request
    ) {
        PromotionResponse response = promotionService.updatePromotion(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate")
    public ResponseEntity<PromotionValidationResponse> validatePromotion(
            @Valid @RequestBody ValidatePromotionRequest request
    ) {
        PromotionValidationResponse response = promotionService.validatePromotion(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/redemptions")
    public ResponseEntity<List<PromotionRedemptionResponse>> getPromotionRedemptions(@PathVariable Long id) {
        List<PromotionRedemptionResponse> responses = promotionService.getPromotionRedemptions(id);
        return ResponseEntity.ok(responses);
    }

    /**
     * Admin: List all promotions across platform or shops
     */
    @GetMapping("/admin")
    public ResponseEntity<List<PromotionResponse>> getPromotionsForAdmin(
            @RequestParam(required = false) com.fooddelivery.core.enums.PromoScope scope,
            @RequestParam(required = false) String status
    ) {
        List<PromotionResponse> responses = promotionService.getAllPromotionsForAdmin(scope, status);
        return ResponseEntity.ok(responses);
    }

    /**
     * Admin: Create platform-wide promotion
     */
    @PostMapping("/admin")
    public ResponseEntity<PromotionResponse> createPlatformPromotion(
            @Valid @RequestBody CreatePromotionRequest request
    ) {
        PromotionResponse response = promotionService.createPlatformPromotion(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Admin: Approve or Reject a shop promotion
     */
    @PostMapping("/admin/{id}/approve")
    public ResponseEntity<PromotionResponse> approveOrRejectPromotion(
            @PathVariable Long id,
            @RequestParam Boolean approved,
            @RequestParam(required = false) String reason
    ) {
        PromotionResponse response = promotionService.approveOrRejectPromotion(id, approved, reason);
        return ResponseEntity.ok(response);
    }
}
