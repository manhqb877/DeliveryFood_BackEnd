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

@CrossOrigin(origins = "*")
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

    @GetMapping("/shop/{shopId}/active")
    public ResponseEntity<List<PromotionResponse>> getActiveShopPromotions(@PathVariable Long shopId) {
        List<PromotionResponse> responses = promotionService.getActiveShopPromotions(shopId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/platform")
    public ResponseEntity<List<PromotionResponse>> getActivePlatformPromotions() {
        List<PromotionResponse> responses = promotionService.getActivePlatformPromotions();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/global")
    public ResponseEntity<List<PromotionResponse>> getActiveGlobalPromotions() {
        List<PromotionResponse> responses = promotionService.getActivePlatformPromotions();
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

    @RequestMapping(value = "/{id}/toggle", method = {RequestMethod.PATCH, RequestMethod.PUT})
    public ResponseEntity<PromotionResponse> togglePromotion(
            @PathVariable Long id,
            @RequestParam(required = false) Boolean isActive,
            @RequestBody(required = false) java.util.Map<String, Object> body
    ) {
        Boolean active = isActive;
        if (active == null && body != null && body.containsKey("isActive")) {
            active = Boolean.valueOf(String.valueOf(body.get("isActive")));
        }
        if (active == null) active = true;
        PromotionResponse response = promotionService.togglePromotionStatus(id, active);
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
    @RequestMapping(value = "/admin/{id}/approve", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<PromotionResponse> approveOrRejectPromotion(
            @PathVariable Long id,
            @RequestParam(required = false) Boolean approved,
            @RequestParam(required = false) String reason,
            @RequestBody(required = false) java.util.Map<String, Object> body
    ) {
        Boolean isApproved = approved;
        String rejReason = reason;
        if (isApproved == null && body != null && body.containsKey("approved")) {
            isApproved = Boolean.valueOf(String.valueOf(body.get("approved")));
        }
        if (rejReason == null && body != null && body.containsKey("rejectionReason")) {
            rejReason = (String) body.get("rejectionReason");
        }
        if (isApproved == null) isApproved = true;
        PromotionResponse response = promotionService.approveOrRejectPromotion(id, isApproved, rejReason);
        return ResponseEntity.ok(response);
    }
}
