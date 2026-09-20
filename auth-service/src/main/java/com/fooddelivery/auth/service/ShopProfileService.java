package com.fooddelivery.auth.service;

import com.fooddelivery.auth.dto.request.ApproveShopRequest;
import com.fooddelivery.auth.dto.request.RegisterShopRequest;
import com.fooddelivery.auth.dto.request.UpdateShopProfileRequest;
import com.fooddelivery.auth.dto.response.ShopProfileResponse;
import com.fooddelivery.auth.enums.ApprovalStatus;

import java.util.List;

public interface ShopProfileService {

    ShopProfileResponse registerShopProfile(RegisterShopRequest request);

    ShopProfileResponse getMyShopProfile(String phone);

    ShopProfileResponse getShopProfileById(Long id);

    List<ShopProfileResponse> getShopsByStatus(ApprovalStatus status);

    List<ShopProfileResponse> getAllShops();

    ShopProfileResponse approveOrRejectShop(Long shopId, ApproveShopRequest request, String adminPhone);
    ShopProfileResponse updateMyShopProfile(String phone, UpdateShopProfileRequest request);

    ShopProfileResponse toggleShopOpenStatus(String phone, Boolean isOpen);

    ShopProfileResponse toggleShopAcceptingOrders(String phone, Boolean isAcceptingOrders);
}
