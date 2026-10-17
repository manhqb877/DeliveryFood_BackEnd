package com.fooddelivery.orders.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(
        name = "core-service",
        url = "${application.services.core-service.url:http://localhost:8082}"
)
public interface CoreServiceClient {

    @org.springframework.web.bind.annotation.GetMapping("/core/shops/{shopId}/details")
    Map<String, Object> getShopDetails(@org.springframework.web.bind.annotation.PathVariable("shopId") Long shopId);

    @PostMapping("/core/items/deduct-stock")
    void deductStock(@RequestBody Map<String, Object> payload);

    @PostMapping("/promotions/redeem")
    Map<String, Object> redeemPromotion(@RequestBody Map<String, Object> payload);

    @org.springframework.web.bind.annotation.GetMapping("/promotions/{id}")
    Map<String, Object> getPromotionDetails(@org.springframework.web.bind.annotation.PathVariable("id") Long id);
}
