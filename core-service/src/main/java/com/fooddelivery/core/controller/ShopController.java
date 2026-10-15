package com.fooddelivery.core.controller;

import com.fooddelivery.core.entity.Shop;
import com.fooddelivery.core.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/core/shops")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    @GetMapping
    public ResponseEntity<List<Shop>> getAllShops() {
        return ResponseEntity.ok(shopService.getAllShops());
    }

    @GetMapping("/{shopId}/details")
    public ResponseEntity<com.fooddelivery.core.dto.response.ShopDetailsResponse> getShopDetails(
            @org.springframework.web.bind.annotation.PathVariable Long shopId) {
        return ResponseEntity.ok(shopService.getShopDetails(shopId));
    }

    @org.springframework.web.bind.annotation.PatchMapping("/{shopId}/status")
    public ResponseEntity<Shop> updateShopStatus(
            @org.springframework.web.bind.annotation.PathVariable Long shopId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Boolean isOpen,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Boolean isAcceptingOrders) {
        return ResponseEntity.ok(shopService.updateShopStatus(shopId, isOpen, isAcceptingOrders));
    }

    @org.springframework.web.bind.annotation.PutMapping("/{shopId}")
    public ResponseEntity<Shop> updateShop(
            @org.springframework.web.bind.annotation.PathVariable Long shopId,
            @org.springframework.web.bind.annotation.RequestBody Shop shopData) {
        return ResponseEntity.ok(shopService.updateShop(shopId, shopData));
    }
}
