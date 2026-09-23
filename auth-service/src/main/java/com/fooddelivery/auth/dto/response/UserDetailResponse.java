package com.fooddelivery.auth.dto.response;

import com.fooddelivery.auth.enums.UserRole;
import com.fooddelivery.auth.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailResponse {
    // 1. Core User fields
    private Long id;
    private String phone;
    private String email;
    private String fullName;
    private String avatarUrl;
    private UserRole role;
    private UserStatus status;
    private Long areaId;
    private Boolean isAreaVerified;
    private Map<String, Object> defaultAddress;
    private List<Map<String, Object>> savedAddresses;
    private OffsetDateTime lastLoginAt;
    private Short failedLoginCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // 2. Area info (if areaId is present)
    private AreaInfo areaInfo;

    // 3. Role specific profiles
    private ShipperProfileResponse shipperProfile;
    private ShopProfileResponse shopProfile;

    // 4. Order statistics & recent orders (via OpenFeign from order-service)
    private UserOrderStatsResponse orderStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AreaInfo {
        private Long id;
        private String areaCode;
        private String areaName;
        private String areaType;
        private String city;
        private String district;
        private String address;
    }
}
