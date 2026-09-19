package com.fooddelivery.orders.service;

import com.fooddelivery.orders.dto.request.AddToCartRequest;
import com.fooddelivery.orders.dto.request.UpdateCartItemRequest;
import com.fooddelivery.orders.dto.response.ApiResponse;

public interface CartService {
    ApiResponse getCart(Long userId, Long guestSessionId, Long shopId);
    ApiResponse getAllCarts(Long userId, Long guestSessionId);
    ApiResponse addToCart(AddToCartRequest request);
    ApiResponse updateCartItem(Long cartItemId, Long userId, Long guestSessionId, UpdateCartItemRequest request);
    ApiResponse removeCartItem(Long cartItemId, Long userId, Long guestSessionId);
    ApiResponse clearCart(Long cartId, Long userId, Long guestSessionId);
}
