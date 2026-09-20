package com.fooddelivery.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterShopRequest {

    // Owner info
    @NotBlank(message = "Owner full name is required")
    private String ownerName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Invalid Vietnamese phone number format")
    private String phone;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address format")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    // Shop Details
    @NotBlank(message = "Shop name is required")
    private String shopName;

    private String shopType;

    private String shopDescription;

    @NotNull(message = "Area ID is required")
    private Long areaId;

    private String locationDetail;
    private String buildingCode;
    private String floor;
    private String unitNumber;
    private Double shopLat;
    private Double shopLng;

    // Media & Contact
    private String logoUrl;
    private String coverImageUrl;
    private String businessLicenseNumber;
    private String foodSafetyCertNumber;
    private String taxId;
    private List<String> documents;

    // Operations
    private List<Map<String, Object>> businessHours;
    private Short avgPrepTimeMinutes;
    private BigDecimal minOrderValue;
}
