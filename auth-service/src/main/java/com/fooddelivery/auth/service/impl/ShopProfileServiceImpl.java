package com.fooddelivery.auth.service.impl;

import com.fooddelivery.auth.dto.request.ApproveShopRequest;
import com.fooddelivery.auth.dto.request.RegisterShopRequest;
import com.fooddelivery.auth.dto.response.ShopProfileResponse;
import com.fooddelivery.auth.entity.Area;
import com.fooddelivery.auth.entity.ShopProfile;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.enums.ApprovalStatus;
import com.fooddelivery.auth.enums.ErrorCode;
import com.fooddelivery.auth.enums.UserRole;
import com.fooddelivery.auth.enums.UserStatus;
import com.fooddelivery.auth.exception.BusinessException;
import com.fooddelivery.auth.repository.AreaRepository;
import com.fooddelivery.auth.repository.ShopProfileRepository;
import com.fooddelivery.auth.repository.UserRepository;
import com.fooddelivery.auth.service.ShopProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopProfileServiceImpl implements ShopProfileService {

    private final ShopProfileRepository shopProfileRepository;
    private final UserRepository userRepository;
    private final AreaRepository areaRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public ShopProfileResponse registerShopProfile(RegisterShopRequest request) {
        log.info("Registering shop profile for phone: {}", request.getPhone());

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
        }
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        Area area = areaRepository.findById(request.getAreaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "Area not found with id: " + request.getAreaId()));

        // Create User account with role SHOP_MANAGER and status PENDING
        User user = User.builder()
                .phone(request.getPhone())
                .email(request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null)
                .fullName(request.getOwnerName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.SHOP_MANAGER)
                .status(UserStatus.PENDING)
                .areaId(area.getId())
                .isAreaVerified(true)
                .failedLoginCount((short) 0)
                .build();

        User savedUser = userRepository.save(user);

        // Map documents list to JSONB map format
        List<Map<String, Object>> docMaps = null;
        if (request.getDocuments() != null && !request.getDocuments().isEmpty()) {
            docMaps = request.getDocuments().stream().map(url -> {
                Map<String, Object> m = new HashMap<>();
                m.put("url", url);
                m.put("type", "DOCUMENT");
                return m;
            }).collect(Collectors.toList());
        }

        // Create Shop Profile with approvalStatus PENDING
        ShopProfile shopProfile = ShopProfile.builder()
                .owner(savedUser)
                .area(area)
                .locationDetail(request.getLocationDetail())
                .shopLat(request.getShopLat() != null ? request.getShopLat() : area.getCenterLat())
                .shopLng(request.getShopLng() != null ? request.getShopLng() : area.getCenterLng())
                .shopName(request.getShopName())
                .shopDescription(request.getShopDescription())
                .logoUrl(request.getLogoUrl())
                .coverImageUrl(request.getCoverImageUrl())
                .phone(request.getPhone())
                .businessHours(request.getBusinessHours())
                .approvalStatus(ApprovalStatus.PENDING)
                .isOpen(false)
                .isAcceptingOrders(false)
                .documents(docMaps)
                .build();

        ShopProfile savedShop = shopProfileRepository.save(shopProfile);
        log.info("Shop profile created with id: {} and approval status PENDING", savedShop.getId());

        return toResponse(savedShop);
    }

    @Override
    public ShopProfileResponse getMyShopProfile(String phone) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ShopProfile shop = shopProfileRepository.findFirstByOwnerId(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "No shop profile found for current user"));

        return toResponse(shop);
    }

    @Override
    public ShopProfileResponse getShopProfileById(Long id) {
        ShopProfile shop = shopProfileRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "Shop profile not found with id: " + id));

        return toResponse(shop);
    }

    @Override
    public List<ShopProfileResponse> getShopsByStatus(ApprovalStatus status) {
        return shopProfileRepository.findByApprovalStatus(status).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ShopProfileResponse> getAllShops() {
        return shopProfileRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ShopProfileResponse approveOrRejectShop(Long shopId, ApproveShopRequest request, String adminPhone) {
        User admin = userRepository.findByPhone(adminPhone)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ShopProfile shop = shopProfileRepository.findById(shopId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "Shop profile not found"));

        User owner = shop.getOwner();

        if (Boolean.TRUE.equals(request.getApproved())) {
            shop.setApprovalStatus(ApprovalStatus.APPROVED);
            shop.setApprovedBy(admin.getId());
            shop.setApprovedAt(OffsetDateTime.now());
            if (request.getCommissionRate() != null) {
                shop.setCommissionRate(request.getCommissionRate());
            }
            shop.setIsOpen(true);
            shop.setIsAcceptingOrders(true);

            // Activate owner account
            owner.setStatus(UserStatus.ACTIVE);
            userRepository.save(owner);

            log.info("Shop id: {} APPROVED by admin id: {}", shopId, admin.getId());
        } else {
            shop.setApprovalStatus(ApprovalStatus.REJECTED);
            shop.setRejectionReason(request.getRejectionReason());
            shop.setApprovedBy(admin.getId());
            shop.setApprovedAt(OffsetDateTime.now());

            // Lock or keep pending
            owner.setStatus(UserStatus.LOCKED);
            userRepository.save(owner);

            log.info("Shop id: {} REJECTED by admin id: {}. Reason: {}", shopId, admin.getId(), request.getRejectionReason());
        }

        ShopProfile updated = shopProfileRepository.save(shop);
        return toResponse(updated);
    }

    @Override
    @Transactional
    public ShopProfileResponse updateMyShopProfile(String phone, com.fooddelivery.auth.dto.request.UpdateShopProfileRequest request) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ShopProfile shop = shopProfileRepository.findFirstByOwnerId(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "No shop profile found for current user"));

        if (request.getShopName() != null) shop.setShopName(request.getShopName());
        if (request.getShopDescription() != null) shop.setShopDescription(request.getShopDescription());
        if (request.getLogoUrl() != null) shop.setLogoUrl(request.getLogoUrl());
        if (request.getCoverImageUrl() != null) shop.setCoverImageUrl(request.getCoverImageUrl());
        if (request.getPhone() != null) shop.setPhone(request.getPhone());
        if (request.getLocationDetail() != null) shop.setLocationDetail(request.getLocationDetail());
        if (request.getShopLat() != null) shop.setShopLat(request.getShopLat());
        if (request.getShopLng() != null) shop.setShopLng(request.getShopLng());
        if (request.getBusinessHours() != null) shop.setBusinessHours(request.getBusinessHours());
        if (request.getIsOpen() != null) shop.setIsOpen(request.getIsOpen());
        if (request.getIsAcceptingOrders() != null) shop.setIsAcceptingOrders(request.getIsAcceptingOrders());

        ShopProfile updated = shopProfileRepository.save(shop);
        log.info("Shop profile updated for shop id: {}", updated.getId());
        return toResponse(updated);
    }

    @Override
    @Transactional
    public ShopProfileResponse toggleShopOpenStatus(String phone, Boolean isOpen) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ShopProfile shop = shopProfileRepository.findFirstByOwnerId(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "No shop profile found for current user"));

        shop.setIsOpen(isOpen);
        ShopProfile updated = shopProfileRepository.save(shop);
        log.info("Shop open status toggled to {} for shop id: {}", isOpen, updated.getId());
        return toResponse(updated);
    }

    @Override
    @Transactional
    public ShopProfileResponse toggleShopAcceptingOrders(String phone, Boolean isAcceptingOrders) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ShopProfile shop = shopProfileRepository.findFirstByOwnerId(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "No shop profile found for current user"));

        shop.setIsAcceptingOrders(isAcceptingOrders);
        ShopProfile updated = shopProfileRepository.save(shop);
        log.info("Shop accepting orders status toggled to {} for shop id: {}", isAcceptingOrders, updated.getId());
        return toResponse(updated);
    }

    private ShopProfileResponse toResponse(ShopProfile s) {
        User owner = s.getOwner();
        Area area = s.getArea();

        return ShopProfileResponse.builder()
                .id(s.getId())
                .ownerId(owner != null ? owner.getId() : null)
                .ownerName(owner != null ? owner.getFullName() : null)
                .ownerPhone(owner != null ? owner.getPhone() : null)
                .ownerEmail(owner != null ? owner.getEmail() : null)
                .areaId(area != null ? area.getId() : null)
                .areaName(area != null ? area.getAreaName() : null)
                .locationDetail(s.getLocationDetail())
                .shopLat(s.getShopLat())
                .shopLng(s.getShopLng())
                .shopName(s.getShopName())
                .shopDescription(s.getShopDescription())
                .logoUrl(s.getLogoUrl())
                .coverImageUrl(s.getCoverImageUrl())
                .phone(s.getPhone())
                .businessHours(s.getBusinessHours())
                .approvalStatus(s.getApprovalStatus())
                .rejectionReason(s.getRejectionReason())
                .isOpen(s.getIsOpen())
                .isAcceptingOrders(s.getIsAcceptingOrders())
                .commissionRate(s.getCommissionRate())
                .documents(s.getDocuments())
                .approvedBy(s.getApprovedBy())
                .approvedAt(s.getApprovedAt())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
