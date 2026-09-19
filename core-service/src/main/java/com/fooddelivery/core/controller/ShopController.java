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
}
