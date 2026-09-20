package com.fooddelivery.auth.dto.request;

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
public class UpdateShopProfileRequest {
    private String shopName;
    private String shopDescription;
    private String logoUrl;
    private String coverImageUrl;
    private String phone;
    private String locationDetail;
    private Double shopLat;
    private Double shopLng;
    private List<Map<String, Object>> businessHours;
    private Boolean isOpen;
    private Boolean isAcceptingOrders;
    private Short avgPrepTimeMinutes;
    private BigDecimal minOrderValue;
}
