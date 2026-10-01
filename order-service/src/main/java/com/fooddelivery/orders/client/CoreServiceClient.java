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

    @PostMapping("/core/items/deduct-stock")
    void deductStock(@RequestBody Map<String, Object> payload);

    @PostMapping("/promotions/redeem")
    Map<String, Object> redeemPromotion(@RequestBody Map<String, Object> payload);
}
