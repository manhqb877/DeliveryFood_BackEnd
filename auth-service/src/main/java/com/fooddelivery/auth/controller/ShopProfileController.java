package com.fooddelivery.auth.controller;

import com.fooddelivery.auth.common.ApiResponse;
import com.fooddelivery.auth.dto.request.ApproveShopRequest;
import com.fooddelivery.auth.dto.request.RegisterShopRequest;
import com.fooddelivery.auth.dto.response.ShopProfileResponse;
import com.fooddelivery.auth.enums.ApprovalStatus;
import com.fooddelivery.auth.service.ShopProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class ShopProfileController {

    private final ShopProfileService shopProfileService;

    /**
     * POST /api/v1/auth/shops/register
     * Register a new Shop Manager account and Shop Profile (Status: PENDING).
     */
    @PostMapping("/shops/register")
    public ResponseEntity<ApiResponse<ShopProfileResponse>> registerShop(
            @Valid @RequestBody RegisterShopRequest request
    ) {
        log.info("Received shop registration request for shop name: {}", request.getShopName());
        ShopProfileResponse response = shopProfileService.registerShopProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Shop profile registered successfully, awaiting Admin approval", response));
    }

    @GetMapping("/shops/{id}")
    public ResponseEntity<ApiResponse<ShopProfileResponse>> getShopById(@PathVariable Long id) {
        ShopProfileResponse response = shopProfileService.getShopProfileById(id);
        return ResponseEntity.ok(ApiResponse.success("Shop profile retrieved successfully", response));
    }

    /**
     * GET /api/v1/auth/shops/me

     * Get shop profile for the currently logged-in Shop Manager.
     */
    @GetMapping("/shops/me")
    @PreAuthorize("hasRole('SHOP_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ShopProfileResponse>> getMyShopProfile(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ShopProfileResponse response = shopProfileService.getMyShopProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Shop profile retrieved successfully", response));
    }

    /**
     * PUT /api/v1/auth/shops/me
     * Update shop profile information for logged-in Shop Manager.
     */
    @PutMapping("/shops/me")
    @PreAuthorize("hasRole('SHOP_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ShopProfileResponse>> updateMyShopProfile(
            @Valid @RequestBody com.fooddelivery.auth.dto.request.UpdateShopProfileRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ShopProfileResponse response = shopProfileService.updateMyShopProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Shop profile updated successfully", response));
    }

    /**
     * PATCH /api/v1/auth/shops/me/open
     * Toggle shop open/close status.
     */
    @PatchMapping("/shops/me/open")
    @PreAuthorize("hasRole('SHOP_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ShopProfileResponse>> toggleShopOpenStatus(
            @RequestParam Boolean isOpen,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ShopProfileResponse response = shopProfileService.toggleShopOpenStatus(userDetails.getUsername(), isOpen);
        return ResponseEntity.ok(ApiResponse.success("Shop open status updated successfully", response));
    }

    /**
     * PATCH /api/v1/auth/shops/me/accepting
     * Toggle shop accepting orders status.
     */
    @PatchMapping("/shops/me/accepting")
    @PreAuthorize("hasRole('SHOP_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ShopProfileResponse>> toggleShopAcceptingOrders(
            @RequestParam Boolean isAcceptingOrders,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ShopProfileResponse response = shopProfileService.toggleShopAcceptingOrders(userDetails.getUsername(), isAcceptingOrders);
        return ResponseEntity.ok(ApiResponse.success("Shop order acceptance status updated successfully", response));
    }

    /**
     * GET /api/v1/auth/admin/shops
     * Admin: List all registered shop profiles (filtered by status if provided).
     */
    @GetMapping("/admin/shops")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ShopProfileResponse>>> getShopsForAdmin(
            @RequestParam(required = false) ApprovalStatus status
    ) {
        List<ShopProfileResponse> response = status != null
                ? shopProfileService.getShopsByStatus(status)
                : shopProfileService.getAllShops();

        return ResponseEntity.ok(ApiResponse.success("Shop profiles retrieved successfully", response));
    }

    /**
     * GET /api/v1/auth/admin/shops/{id}
     * Admin: Get shop profile details by ID.
     */
    @GetMapping("/admin/shops/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ShopProfileResponse>> getShopByIdForAdmin(
            @PathVariable Long id
    ) {
        ShopProfileResponse response = shopProfileService.getShopProfileById(id);
        return ResponseEntity.ok(ApiResponse.success("Shop profile details retrieved successfully", response));
    }

    /**
     * POST /api/v1/auth/admin/shops/{id}/approve
     * Admin: Approve or Reject a pending shop profile registration.
     */
    @PostMapping("/admin/shops/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ShopProfileResponse>> approveOrRejectShop(
            @PathVariable Long id,
            @Valid @RequestBody ApproveShopRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ShopProfileResponse response = shopProfileService.approveOrRejectShop(id, request, userDetails.getUsername());
        String msg = Boolean.TRUE.equals(request.getApproved())
                ? "Shop approved successfully"
                : "Shop rejected successfully";
        return ResponseEntity.ok(ApiResponse.success(msg, response));
    }
}
