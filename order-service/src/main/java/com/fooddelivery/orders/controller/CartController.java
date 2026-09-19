package com.fooddelivery.orders.controller;

import com.fooddelivery.orders.dto.request.AddToCartRequest;
import com.fooddelivery.orders.dto.request.UpdateCartItemRequest;
import com.fooddelivery.orders.dto.response.ApiResponse;
import com.fooddelivery.orders.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/carts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CartController {

    private final CartService cartService;

    /**
     * GET /carts?userId=&guestSessionId=&shopId=
     * Lấy giỏ hàng của user/guest theo shop
     */
    @GetMapping
    public ResponseEntity<ApiResponse> getCart(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long guestSessionId,
            @RequestParam(required = false) Long shopId) {
        ApiResponse response = cartService.getCart(userId, guestSessionId, shopId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /carts/all?userId=&guestSessionId=
     * Lấy tất cả giỏ hàng (mọi shop)
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse> getAllCarts(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long guestSessionId) {
        ApiResponse response = cartService.getAllCarts(userId, guestSessionId);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /carts/items
     * Thêm món vào giỏ hàng
     */
    @PostMapping("/items")
    public ResponseEntity<ApiResponse> addToCart(@RequestBody AddToCartRequest request) {
        ApiResponse response = cartService.addToCart(request);
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /carts/items/{cartItemId}?userId=&guestSessionId=
     * Cập nhật số lượng/options của 1 món trong giỏ
     */
    @PatchMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse> updateCartItem(
            @PathVariable Long cartItemId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long guestSessionId,
            @RequestBody UpdateCartItemRequest request) {
        ApiResponse response = cartService.updateCartItem(cartItemId, userId, guestSessionId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /carts/items/{cartItemId}?userId=&guestSessionId=
     * Xóa 1 món khỏi giỏ
     */
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse> removeCartItem(
            @PathVariable Long cartItemId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long guestSessionId) {
        ApiResponse response = cartService.removeCartItem(cartItemId, userId, guestSessionId);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /carts/{cartId}?userId=&guestSessionId=
     * Xóa toàn bộ giỏ hàng
     */
    @DeleteMapping("/{cartId}")
    public ResponseEntity<ApiResponse> clearCart(
            @PathVariable Long cartId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long guestSessionId) {
        ApiResponse response = cartService.clearCart(cartId, userId, guestSessionId);
        return ResponseEntity.ok(response);
    }
}
