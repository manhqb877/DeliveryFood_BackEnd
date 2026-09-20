package com.fooddelivery.core.service.impl;

import com.fooddelivery.core.dto.request.CreatePromotionRequest;
import com.fooddelivery.core.dto.request.ValidatePromotionRequest;
import com.fooddelivery.core.dto.response.PromotionRedemptionResponse;
import com.fooddelivery.core.dto.response.PromotionResponse;
import com.fooddelivery.core.dto.response.PromotionValidationResponse;
import com.fooddelivery.core.entity.Promotion;
import com.fooddelivery.core.entity.PromotionRedemption;
import com.fooddelivery.core.enums.PromoScope;
import com.fooddelivery.core.enums.PromoType;
import com.fooddelivery.core.enums.RedemptionStatus;
import com.fooddelivery.core.exception.ResourceNotFoundException;
import com.fooddelivery.core.repository.PromotionRedemptionRepository;
import com.fooddelivery.core.repository.PromotionRepository;
import com.fooddelivery.core.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionRedemptionRepository redemptionRepository;

    @Override
    public List<PromotionResponse> getShopPromotions(Long shopId) {
        return promotionRepository.findByShopIdOrderByCreatedAtDesc(shopId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PromotionResponse> getActiveShopPromotions(Long shopId) {
        OffsetDateTime now = OffsetDateTime.now();
        return promotionRepository.findByShopIdAndIsActiveTrueAndApprovalStatus(shopId, "APPROVED")
                .stream()
                .filter(p -> p.getValidUntil() == null || !p.getValidUntil().isBefore(now))
                .filter(p -> p.getValidFrom() == null || p.getValidFrom().minusHours(24).isBefore(now))
                .filter(p -> p.getTotalLimit() == null || p.getUsedCount() == null || p.getUsedCount() < p.getTotalLimit())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PromotionResponse> getActivePlatformPromotions() {
        OffsetDateTime now = OffsetDateTime.now();
        List<PromoScope> scopes = List.of(PromoScope.PLATFORM, PromoScope.AREA);
        return promotionRepository.findByScopeInAndIsActiveTrueAndApprovalStatus(scopes, "APPROVED")
                .stream()
                .filter(p -> p.getValidUntil() == null || !p.getValidUntil().isBefore(now))
                .filter(p -> p.getValidFrom() == null || p.getValidFrom().minusHours(24).isBefore(now))
                .filter(p -> p.getTotalLimit() == null || p.getUsedCount() == null || p.getUsedCount() < p.getTotalLimit())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PromotionResponse createShopPromotion(Long shopId, CreatePromotionRequest request) {
        String normalizedCode = request.getCode().trim().toUpperCase().replaceAll("\\s+", "");

        if (promotionRepository.existsByCode(normalizedCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã khuyến mãi '" + normalizedCode + "' đã tồn tại!");
        }

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime validFrom = request.getValidFrom() != null ? request.getValidFrom() : now;
        OffsetDateTime validUntil = request.getValidUntil();

        if (validUntil.isBefore(validFrom)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời hạn kết thúc phải sau thời gian bắt đầu!");
        }

        // Anti-abuse: Quota & Per user defaults
        int totalLimit = request.getTotalLimit() != null && request.getTotalLimit() > 0 ? request.getTotalLimit() : 20;
        short perUserLimit = request.getPerUserLimit() != null && request.getPerUserLimit() > 0 ? request.getPerUserLimit() : (short) 1;
        BigDecimal minOrder = request.getMinOrderValue() != null ? request.getMinOrderValue() : BigDecimal.ZERO;

        // Shop promotions require Admin approval by default
        String approvalStatus = "PENDING";

        Promotion promo = Promotion.builder()
                .code(normalizedCode)
                .promoType(request.getPromoType())
                .scope(PromoScope.SHOP)
                .shopId(shopId)
                .discountValue(request.getDiscountValue())
                .minOrderValue(minOrder)
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .totalLimit(totalLimit)
                .perUserLimit(perUserLimit)
                .usedCount(0)
                .applicableTo(request.getApplicableTo() != null ? request.getApplicableTo() : com.fooddelivery.core.enums.ApplicableTo.ALL)
                .validFrom(validFrom)
                .validUntil(validUntil)
                .approvalStatus(approvalStatus)
                .isActive(true)
                .build();

        Promotion saved = promotionRepository.save(promo);
        log.info("Created new shop promotion: {} for shop ID: {}, total quota: {}", saved.getCode(), shopId, totalLimit);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PromotionResponse togglePromotionStatus(Long promotionId, Boolean isActive) {
        Promotion promo = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã khuyến mãi với ID: " + promotionId));

        promo.setIsActive(isActive);
        Promotion updated = promotionRepository.save(promo);
        log.info("Promotion {} status toggled to {}", promo.getCode(), isActive);
        return toResponse(updated);
    }

    @Override
    public PromotionValidationResponse validatePromotion(ValidatePromotionRequest request) {
        String normalizedCode = request.getCode().trim().toUpperCase().replaceAll("\\s+", "");
        BigDecimal orderAmount = request.getOrderAmount() != null ? request.getOrderAmount() : BigDecimal.ZERO;

        Promotion promo = promotionRepository.findByCode(normalizedCode).orElse(null);

        if (promo == null) {
            return PromotionValidationResponse.builder()
                    .valid(false)
                    .code(normalizedCode)
                    .message("Mã khuyến mãi không tồn tại trong hệ thống.")
                    .originalAmount(orderAmount)
                    .discountAmount(BigDecimal.ZERO)
                    .finalAmount(orderAmount)
                    .build();
        }

        if (Boolean.FALSE.equals(promo.getIsActive())) {
            return PromotionValidationResponse.builder()
                    .valid(false)
                    .code(normalizedCode)
                    .message("Mã khuyến mãi hiện đang tạm dừng hoặc không hoạt động.")
                    .originalAmount(orderAmount)
                    .discountAmount(BigDecimal.ZERO)
                    .finalAmount(orderAmount)
                    .build();
        }

        if (!"APPROVED".equalsIgnoreCase(promo.getApprovalStatus())) {
            return PromotionValidationResponse.builder()
                    .valid(false)
                    .code(normalizedCode)
                    .message("Mã khuyến mãi đang chờ Ban Quản Trị phê duyệt.")
                    .originalAmount(orderAmount)
                    .discountAmount(BigDecimal.ZERO)
                    .finalAmount(orderAmount)
                    .build();
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (now.isBefore(promo.getValidFrom())) {
            return PromotionValidationResponse.builder()
                    .valid(false)
                    .code(normalizedCode)
                    .message("Mã khuyến mãi chưa đến ngày bắt đầu có hiệu lực.")
                    .originalAmount(orderAmount)
                    .discountAmount(BigDecimal.ZERO)
                    .finalAmount(orderAmount)
                    .build();
        }

        if (now.isAfter(promo.getValidUntil())) {
            return PromotionValidationResponse.builder()
                    .valid(false)
                    .code(normalizedCode)
                    .message("Mã khuyến mãi đã hết hạn sử dụng.")
                    .originalAmount(orderAmount)
                    .discountAmount(BigDecimal.ZERO)
                    .finalAmount(orderAmount)
                    .build();
        }

        if (promo.getShopId() != null && request.getShopId() != null && !promo.getShopId().equals(request.getShopId())) {
            return PromotionValidationResponse.builder()
                    .valid(false)
                    .code(normalizedCode)
                    .message("Mã khuyến mãi này chỉ áp dụng cho gian hàng riêng biệt, không áp dụng cho quán này.")
                    .originalAmount(orderAmount)
                    .discountAmount(BigDecimal.ZERO)
                    .finalAmount(orderAmount)
                    .build();
        }

        // ================= ANTI-ABUSE CHECK 1: Quota Limit (Tổng số mã phát hành) =================
        int totalLimit = promo.getTotalLimit() != null ? promo.getTotalLimit() : Integer.MAX_VALUE;
        int usedCount = promo.getUsedCount() != null ? promo.getUsedCount() : 0;
        int remainingQuota = Math.max(0, totalLimit - usedCount);

        if (usedCount >= totalLimit) {
            return PromotionValidationResponse.builder()
                    .valid(false)
                    .code(normalizedCode)
                    .remainingQuota(0)
                    .message("Mã khuyến mãi đã hết lượt sử dụng trên toàn hệ thống (Đã phát hết " + totalLimit + " mã).")
                    .originalAmount(orderAmount)
                    .discountAmount(BigDecimal.ZERO)
                    .finalAmount(orderAmount)
                    .build();
        }

        // ================= ANTI-ABUSE CHECK 2: Per-User Abuse Limit =================
        short perUserLimit = promo.getPerUserLimit() != null ? promo.getPerUserLimit() : (short) 1;
        long userUsageCount = 0;
        if (request.getUserId() != null) {
            userUsageCount = redemptionRepository.countByPromotionIdAndUserId(promo.getId(), request.getUserId());
        } else if (request.getGuestSessionId() != null) {
            userUsageCount = redemptionRepository.countByPromotionIdAndGuestSessionId(promo.getId(), request.getGuestSessionId());
        }

        if (userUsageCount >= perUserLimit) {
            return PromotionValidationResponse.builder()
                    .valid(false)
                    .code(normalizedCode)
                    .remainingQuota(remainingQuota)
                    .message("Bạn đã sử dụng hết số lần cho phép của mã này (Tối đa " + perUserLimit + " lần / khách hàng).")
                    .originalAmount(orderAmount)
                    .discountAmount(BigDecimal.ZERO)
                    .finalAmount(orderAmount)
                    .build();
        }

        // ================= ANTI-ABUSE CHECK 3: Minimum Order Value =================
        BigDecimal minOrderValue = promo.getMinOrderValue() != null ? promo.getMinOrderValue() : BigDecimal.ZERO;
        if (orderAmount.compareTo(minOrderValue) < 0) {
            return PromotionValidationResponse.builder()
                    .valid(false)
                    .code(normalizedCode)
                    .remainingQuota(remainingQuota)
                    .message("Đơn hàng tối thiểu phải từ " + String.format("%,.0f", minOrderValue) + " ₫ để được áp dụng mã này.")
                    .originalAmount(orderAmount)
                    .discountAmount(BigDecimal.ZERO)
                    .finalAmount(orderAmount)
                    .build();
        }

        // Calculate discount amount
        BigDecimal discount = BigDecimal.ZERO;
        if (promo.getPromoType() == PromoType.FIXED_AMOUNT) {
            discount = promo.getDiscountValue();
        } else if (promo.getPromoType() == PromoType.PERCENT) {
            discount = orderAmount.multiply(promo.getDiscountValue()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            // Cap discount at maxDiscountAmount if configured
            if (promo.getMaxDiscountAmount() != null && promo.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                discount = discount.min(promo.getMaxDiscountAmount());
            }
        } else if (promo.getPromoType() == PromoType.FREE_DELIVERY) {
            discount = promo.getDiscountValue() != null ? promo.getDiscountValue() : new BigDecimal("15000");
        }

        discount = discount.min(orderAmount);
        BigDecimal finalAmount = orderAmount.subtract(discount).max(BigDecimal.ZERO);

        return PromotionValidationResponse.builder()
                .valid(true)
                .code(normalizedCode)
                .promoType(promo.getPromoType().name())
                .discountAmount(discount)
                .originalAmount(orderAmount)
                .finalAmount(finalAmount)
                .remainingQuota(remainingQuota)
                .message("Áp dụng mã thành công! Giảm " + String.format("%,.0f", discount) + " ₫ vào đơn hàng.")
                .build();
    }

    @Override
    @Transactional
    public PromotionRedemptionResponse redeemPromotion(Long promotionId, Long orderId, Long userId, Long guestSessionId, BigDecimal orderAmount) {
        Promotion promo = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found with id: " + promotionId));

        ValidatePromotionRequest validateReq = ValidatePromotionRequest.builder()
                .code(promo.getCode())
                .shopId(promo.getShopId())
                .userId(userId)
                .guestSessionId(guestSessionId)
                .orderAmount(orderAmount)
                .build();

        PromotionValidationResponse validation = validatePromotion(validateReq);
        if (!validation.isValid()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, validation.getMessage());
        }

        // Increment used count
        promo.setUsedCount((promo.getUsedCount() != null ? promo.getUsedCount() : 0) + 1);
        promotionRepository.save(promo);

        PromotionRedemption redemption = PromotionRedemption.builder()
                .promotion(promo)
                .userId(userId)
                .guestSessionId(guestSessionId)
                .orderId(orderId)
                .discountApplied(validation.getDiscountAmount())
                .status(RedemptionStatus.USED)
                .build();

        PromotionRedemption saved = redemptionRepository.save(redemption);
        log.info("Redeemed promotion {} for order ID {}, discount: {}", promo.getCode(), orderId, validation.getDiscountAmount());

        return PromotionRedemptionResponse.builder()
                .id(saved.getId())
                .promotionId(promo.getId())
                .promoCode(promo.getCode())
                .userId(userId)
                .guestSessionId(guestSessionId)
                .orderId(orderId)
                .discountApplied(saved.getDiscountApplied())
                .status(saved.getStatus().name())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    public List<PromotionRedemptionResponse> getPromotionRedemptions(Long promotionId) {
        return redemptionRepository.findByPromotionIdOrderByCreatedAtDesc(promotionId)
                .stream()
                .map(r -> PromotionRedemptionResponse.builder()
                        .id(r.getId())
                        .promotionId(r.getPromotion().getId())
                        .promoCode(r.getPromotion().getCode())
                        .userId(r.getUserId())
                        .guestSessionId(r.getGuestSessionId())
                        .orderId(r.getOrderId())
                        .discountApplied(r.getDiscountApplied())
                        .status(r.getStatus().name())
                        .createdAt(r.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<PromotionResponse> getAllPromotionsForAdmin(PromoScope scope, String approvalStatus) {
        return promotionRepository.findAll().stream()
                .filter(p -> scope == null || p.getScope() == scope)
                .filter(p -> approvalStatus == null || approvalStatus.equalsIgnoreCase("ALL") || approvalStatus.equalsIgnoreCase(p.getApprovalStatus()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PromotionResponse createPlatformPromotion(CreatePromotionRequest request) {
        String normalizedCode = request.getCode().trim().toUpperCase().replaceAll("\\s+", "");

        if (promotionRepository.existsByCode(normalizedCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã khuyến mãi '" + normalizedCode + "' đã tồn tại!");
        }

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime validFrom = request.getValidFrom() != null ? request.getValidFrom() : now;
        OffsetDateTime validUntil = request.getValidUntil();

        int totalLimit = request.getTotalLimit() != null && request.getTotalLimit() > 0 ? request.getTotalLimit() : 1000;
        short perUserLimit = request.getPerUserLimit() != null && request.getPerUserLimit() > 0 ? request.getPerUserLimit() : (short) 1;
        BigDecimal minOrder = request.getMinOrderValue() != null ? request.getMinOrderValue() : BigDecimal.ZERO;

        Promotion promo = Promotion.builder()
                .code(normalizedCode)
                .promoType(request.getPromoType())
                .scope(PromoScope.PLATFORM)
                .shopId(null)
                .discountValue(request.getDiscountValue())
                .minOrderValue(minOrder)
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .totalLimit(totalLimit)
                .perUserLimit(perUserLimit)
                .usedCount(0)
                .applicableTo(request.getApplicableTo() != null ? request.getApplicableTo() : com.fooddelivery.core.enums.ApplicableTo.ALL)
                .validFrom(validFrom)
                .validUntil(validUntil)
                .approvalStatus("APPROVED") // Platform promos are pre-approved
                .isActive(true)
                .build();

        Promotion saved = promotionRepository.save(promo);
        log.info("Created new PLATFORM promotion: {} with limit: {}", saved.getCode(), totalLimit);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PromotionResponse updatePromotion(Long id, CreatePromotionRequest request) {
        Promotion promo = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã khuyến mãi ID: " + id));

        if (request.getDiscountValue() != null) {
            promo.setDiscountValue(request.getDiscountValue());
        }
        if (request.getMinOrderValue() != null) {
            promo.setMinOrderValue(request.getMinOrderValue());
        }
        if (request.getMaxDiscountAmount() != null) {
            promo.setMaxDiscountAmount(request.getMaxDiscountAmount());
        }
        if (request.getTotalLimit() != null && request.getTotalLimit() > 0) {
            promo.setTotalLimit(request.getTotalLimit());
        }
        if (request.getPerUserLimit() != null && request.getPerUserLimit() > 0) {
            promo.setPerUserLimit(request.getPerUserLimit());
        }
        if (request.getApplicableTo() != null) {
            promo.setApplicableTo(request.getApplicableTo());
        }
        if (request.getValidFrom() != null) {
            promo.setValidFrom(request.getValidFrom());
        }
        if (request.getValidUntil() != null) {
            promo.setValidUntil(request.getValidUntil());
        }
        if (request.getPromoType() != null) {
            promo.setPromoType(request.getPromoType());
        }

        Promotion updated = promotionRepository.save(promo);
        log.info("Promotion {} (ID: {}) updated successfully", promo.getCode(), id);
        return toResponse(updated);
    }

    @Override
    @Transactional
    public PromotionResponse approveOrRejectPromotion(Long id, Boolean approved, String rejectionReason) {
        Promotion promo = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã khuyến mãi ID: " + id));

        if (Boolean.TRUE.equals(approved)) {
            promo.setApprovalStatus("APPROVED");
            promo.setIsActive(true);
            log.info("Promotion {} APPROVED by Admin", promo.getCode());
        } else {
            promo.setApprovalStatus("REJECTED");
            promo.setIsActive(false);
            log.info("Promotion {} REJECTED by Admin. Reason: {}", promo.getCode(), rejectionReason);
        }

        Promotion updated = promotionRepository.save(promo);
        return toResponse(updated);
    }

    private PromotionResponse toResponse(Promotion p) {
        int totalLimit = p.getTotalLimit() != null ? p.getTotalLimit() : 0;
        int usedCount = p.getUsedCount() != null ? p.getUsedCount() : 0;

        return PromotionResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .promoType(p.getPromoType())
                .scope(p.getScope())
                .shopId(p.getShopId())
                .areaId(p.getAreaId())
                .discountValue(p.getDiscountValue())
                .minOrderValue(p.getMinOrderValue())
                .maxDiscountAmount(p.getMaxDiscountAmount())
                .totalLimit(p.getTotalLimit())
                .perUserLimit(p.getPerUserLimit())
                .usedCount(usedCount)
                .remainingQuota(Math.max(0, totalLimit - usedCount))
                .applicableTo(p.getApplicableTo())
                .validFrom(p.getValidFrom())
                .validUntil(p.getValidUntil())
                .approvalStatus(p.getApprovalStatus())
                .isActive(p.getIsActive())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
