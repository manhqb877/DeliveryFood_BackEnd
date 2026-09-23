package com.fooddelivery.auth.dto.request;

import com.fooddelivery.auth.enums.UserRole;
import com.fooddelivery.auth.enums.UserStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdateUserRequest {

    private String phone;
    private String email;
    private String fullName;
    private String avatarUrl;
    private UserRole role;
    private UserStatus status;
    private Long areaId;
    private Boolean isAreaVerified;
    private Map<String, Object> defaultAddress;

    // Shipper specific profile fields
    private String idCardNumber;
    private String vehicleType;
    private String vehiclePlate;
    private String shipperApprovalStatus;

    // Shop specific profile fields
    private String shopName;
    private String locationDetail;
    private String shopType;
    private String shopApprovalStatus;
}
