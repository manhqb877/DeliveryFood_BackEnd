package com.fooddelivery.auth.service.impl;

import com.fooddelivery.auth.client.OrderServiceClient;
import com.fooddelivery.auth.dto.request.AdminUpdateUserRequest;
import com.fooddelivery.auth.dto.response.*;
import com.fooddelivery.auth.entity.Area;
import com.fooddelivery.auth.entity.ShipperProfile;
import com.fooddelivery.auth.entity.ShopProfile;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.enums.ApprovalStatus;
import com.fooddelivery.auth.enums.UserRole;
import com.fooddelivery.auth.enums.UserStatus;
import com.fooddelivery.auth.enums.VehicleType;
import com.fooddelivery.auth.exception.ResourceNotFoundException;
import com.fooddelivery.auth.kafka.UserEventProducer;
import com.fooddelivery.auth.repository.AreaRepository;
import com.fooddelivery.auth.repository.ShipperProfileRepository;
import com.fooddelivery.auth.repository.ShopProfileRepository;
import com.fooddelivery.auth.repository.UserRepository;
import com.fooddelivery.auth.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final AreaRepository areaRepository;
    private final ShipperProfileRepository shipperProfileRepository;
    private final ShopProfileRepository shopProfileRepository;
    private final OrderServiceClient orderServiceClient;
    private final UserEventProducer userEventProducer;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<UserResponse> getAllUsers(String keyword, UserRole role, UserStatus status) {
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) 
                ? removeVietnameseTones(keyword.trim()) 
                : null;

        return userRepository.findAll().stream()
                .filter(u -> role == null || u.getRole() == role)
                .filter(u -> status == null || u.getStatus() == status)
                .filter(u -> {
                    if (cleanKeyword == null) return true;
                    String phone = u.getPhone() != null ? u.getPhone().toLowerCase() : "";
                    String name = u.getFullName() != null ? removeVietnameseTones(u.getFullName()) : "";
                    String email = u.getEmail() != null ? u.getEmail().toLowerCase() : "";
                    return phone.contains(cleanKeyword) || name.contains(cleanKeyword) || email.contains(cleanKeyword);
                })
                .sorted((a, b) -> Long.compare(b.getId() != null ? b.getId() : 0, a.getId() != null ? a.getId() : 0))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UserDetailResponse getUserDetail(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return buildUserDetailResponse(user);
    }

    @Override
    @Transactional
    public UserDetailResponse updateUserDetail(Long id, AdminUpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // 1. Cập nhật các trường cơ bản của User
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getEmail() != null) {
            String newEmail = request.getEmail().trim().toLowerCase();
            if (!newEmail.equals(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
                throw new IllegalArgumentException("Email đã được sử dụng bởi tài khoản khác: " + newEmail);
            }
            user.setEmail(newEmail.isEmpty() ? null : newEmail);
        }
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            String newPhone = request.getPhone().trim();
            if (!newPhone.equals(user.getPhone()) && userRepository.existsByPhone(newPhone)) {
                throw new IllegalArgumentException("Số điện thoại đã được sử dụng bởi tài khoản khác: " + newPhone);
            }
            user.setPhone(newPhone);
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        if (request.getAreaId() != null) {
            user.setAreaId(request.getAreaId());
        }
        if (request.getIsAreaVerified() != null) {
            user.setIsAreaVerified(request.getIsAreaVerified());
        }
        if (request.getDefaultAddress() != null) {
            user.setDefaultAddress(request.getDefaultAddress());
        }

        user = userRepository.save(user);

        // 2. Cập nhật ShipperProfile nếu là role SHIPPER hoặc có trường shipper
        if (user.getRole() == UserRole.SHIPPER) {
            ShipperProfile shipper = shipperProfileRepository.findByUserId(user.getId()).orElse(null);
            if (shipper == null) {
                shipper = ShipperProfile.builder()
                        .user(user)
                        .approvalStatus(ApprovalStatus.PENDING)
                        .totalDeliveries(0)
                        .avgRating(new BigDecimal("5.00"))
                        .build();
            }
            if (request.getIdCardNumber() != null) {
                shipper.setIdCardNumber(request.getIdCardNumber().trim());
            }
            if (request.getVehiclePlate() != null) {
                shipper.setVehiclePlate(request.getVehiclePlate().trim());
            }
            if (request.getVehicleType() != null) {
                try {
                    shipper.setVehicleType(VehicleType.valueOf(request.getVehicleType().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown vehicle type: {}", request.getVehicleType());
                }
            }
            if (request.getShipperApprovalStatus() != null) {
                try {
                    shipper.setApprovalStatus(ApprovalStatus.valueOf(request.getShipperApprovalStatus().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    log.warn("Unknown approval status: {}", request.getShipperApprovalStatus());
                }
            }
            shipperProfileRepository.save(shipper);
        }

        // 3. Cập nhật ShopProfile nếu là role SHOP_MANAGER hoặc có trường shop
        if (user.getRole() == UserRole.SHOP_MANAGER) {
            ShopProfile shop = shopProfileRepository.findFirstByOwnerId(user.getId()).orElse(null);
            if (shop != null) {
                if (request.getShopName() != null && !request.getShopName().trim().isEmpty()) {
                    shop.setShopName(request.getShopName().trim());
                }
                if (request.getLocationDetail() != null) {
                    shop.setLocationDetail(request.getLocationDetail());
                }
                if (request.getShopApprovalStatus() != null) {
                    try {
                        shop.setApprovalStatus(ApprovalStatus.valueOf(request.getShopApprovalStatus().toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        log.warn("Unknown shop approval status: {}", request.getShopApprovalStatus());
                    }
                }
                shopProfileRepository.save(shop);
            }
        }

        // 4. Bắn Kafka event thông báo cập nhật User
        userEventProducer.publishUserUpdated(user, "ADMIN_UPDATED");

        return buildUserDetailResponse(user);
    }

    private UserDetailResponse buildUserDetailResponse(User user) {
        // Area Info
        UserDetailResponse.AreaInfo areaInfo = null;
        if (user.getAreaId() != null) {
            areaInfo = areaRepository.findById(user.getAreaId())
                    .map(a -> UserDetailResponse.AreaInfo.builder()
                            .id(a.getId())
                            .areaCode(a.getAreaCode())
                            .areaName(a.getAreaName())
                            .areaType(a.getAreaType() != null ? a.getAreaType().name() : null)
                            .city(a.getCity())
                            .district(a.getDistrict())
                            .address(a.getAddress())
                            .build())
                    .orElse(null);
        }

        // Shipper Profile
        ShipperProfileResponse shipperProfile = null;
        if (user.getRole() == UserRole.SHIPPER) {
            shipperProfile = shipperProfileRepository.findByUserId(user.getId())
                    .map(s -> ShipperProfileResponse.builder()
                            .id(s.getId())
                            .user_id(user.getId())
                            .full_name(user.getFullName())
                            .phone(user.getPhone())
                            .id_card_number(s.getIdCardNumber())
                            .vehicle_type(s.getVehicleType() != null ? s.getVehicleType().name() : null)
                            .vehicle_plate(s.getVehiclePlate())
                            .vehicle_photo_url(s.getVehiclePhotoUrl())
                            .registered_area_ids(s.getRegisteredAreaIds())
                            .approval_status(s.getApprovalStatus() != null ? s.getApprovalStatus().name() : "PENDING")
                            .avg_rating(s.getAvgRating())
                            .total_deliveries(s.getTotalDeliveries())
                            .rejection_reason(s.getRejectionReason())
                            .created_at(s.getCreatedAt())
                            .build())
                    .orElse(null);
        }

        // Shop Profile
        ShopProfileResponse shopProfile = null;
        if (user.getRole() == UserRole.SHOP_MANAGER) {
            shopProfile = shopProfileRepository.findFirstByOwnerId(user.getId())
                    .map(s -> ShopProfileResponse.builder()
                            .id(s.getId())
                            .ownerId(user.getId())
                            .ownerName(user.getFullName())
                            .ownerPhone(user.getPhone())
                            .ownerEmail(user.getEmail())
                            .areaId(s.getArea() != null ? s.getArea().getId() : null)
                            .areaName(s.getArea() != null ? s.getArea().getAreaName() : null)
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
                            .build())
                    .orElse(null);
        }

        // Order Statistics & Recent Orders via OpenFeign from order-service
        UserOrderStatsResponse orderStats = fetchUserOrderStatsViaFeign(user.getId());

        return UserDetailResponse.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .status(user.getStatus())
                .areaId(user.getAreaId())
                .isAreaVerified(user.getIsAreaVerified())
                .defaultAddress(user.getDefaultAddress())
                .savedAddresses(user.getSavedAddresses())
                .lastLoginAt(user.getLastLoginAt())
                .failedLoginCount(user.getFailedLoginCount())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .areaInfo(areaInfo)
                .shipperProfile(shipperProfile)
                .shopProfile(shopProfile)
                .orderStats(orderStats)
                .build();
    }

    private UserOrderStatsResponse fetchUserOrderStatsViaFeign(Long userId) {
        try {
            List<UserOrderResponse> userOrders = orderServiceClient.getUserOrders(userId);
            if (userOrders == null || userOrders.isEmpty()) {
                userOrders = orderServiceClient.getOrdersByUserId(userId);
            }
            if (userOrders != null && !userOrders.isEmpty()) {
                int totalOrders = userOrders.size();
                BigDecimal totalSpent = BigDecimal.ZERO;
                int completed = 0;
                int cancelled = 0;
                for (UserOrderResponse o : userOrders) {
                    if (o.getTotalAmount() != null) {
                        totalSpent = totalSpent.add(o.getTotalAmount());
                    }
                    String st = o.getOrderStatus() != null ? o.getOrderStatus().toUpperCase() : "";
                    if ("COMPLETED".equals(st) || "DELIVERED".equals(st)) {
                        completed++;
                    } else if ("CANCELLED".equals(st)) {
                        cancelled++;
                    }
                }
                List<UserOrderResponse> recent = userOrders.stream()
                        .sorted((a, b) -> {
                            if (a.getPlacedAt() == null || b.getPlacedAt() == null) return 0;
                            return b.getPlacedAt().compareTo(a.getPlacedAt());
                        })
                        .limit(5)
                        .collect(Collectors.toList());

                return UserOrderStatsResponse.builder()
                        .totalOrders(totalOrders)
                        .totalSpent(totalSpent)
                        .completedOrders(completed)
                        .cancelledOrders(cancelled)
                        .recentOrders(recent)
                        .build();
            }
        } catch (Exception ex) {
            log.warn("OpenFeign call to order-service failed for userId={}: {}", userId, ex.getMessage());
        }

        return UserOrderStatsResponse.builder()
                .totalOrders(0)
                .totalSpent(BigDecimal.ZERO)
                .completedOrders(0)
                .cancelledOrders(0)
                .recentOrders(Collections.emptyList())
                .build();
    }

    private String removeVietnameseTones(String str) {
        if (str == null) return "";
        String nfd = java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD);
        return java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
                .matcher(nfd)
                .replaceAll("")
                .replace("đ", "d")
                .replace("Đ", "d")
                .toLowerCase();
    }

    @Override
    public void updateUserStatus(Long id, UserStatus status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setStatus(status);
        userRepository.save(user);

        // Bắn Kafka event thông báo trạng thái tài khoản thay đổi
        userEventProducer.publishUserUpdated(user, "STATUS_CHANGED");
    }

    @Override
    public void resetUserPassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Bắn Kafka event
        userEventProducer.publishUserUpdated(user, "PASSWORD_RESET");
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .status(user.getStatus())
                .areaId(user.getAreaId())
                .isAreaVerified(user.getIsAreaVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
