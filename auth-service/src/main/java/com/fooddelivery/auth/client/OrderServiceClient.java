package com.fooddelivery.auth.client;

import com.fooddelivery.auth.dto.response.UserOrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(
        name = "order-service",
        url = "${application.services.order-service.url:http://localhost:8083}"
)
public interface OrderServiceClient {

    @GetMapping("/orders")
    List<UserOrderResponse> getUserOrders(
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    );

    @GetMapping("/orders/user/{userId}")
    List<UserOrderResponse> getOrdersByUserId(
            @PathVariable("userId") Long userId
    );
}
