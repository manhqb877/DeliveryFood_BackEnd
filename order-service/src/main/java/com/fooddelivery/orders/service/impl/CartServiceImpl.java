package com.fooddelivery.orders.service.impl;

import com.fooddelivery.orders.dto.request.AddToCartRequest;
import com.fooddelivery.orders.dto.request.UpdateCartItemRequest;
import com.fooddelivery.orders.dto.response.ApiResponse;
import com.fooddelivery.orders.entity.Cart;
import com.fooddelivery.orders.entity.CartItem;
import com.fooddelivery.orders.exception.ResourceNotFoundException;
import com.fooddelivery.orders.repository.CartItemRepository;
import com.fooddelivery.orders.repository.CartRepository;
import com.fooddelivery.orders.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    @Override
    public ApiResponse getCart(Long userId, Long guestSessionId, Long shopId) {
        Optional<Cart> cartOpt = findCart(userId, guestSessionId, shopId);
        if (cartOpt.isEmpty()) {
            return ApiResponse.builder().status(200).message("Cart not found").data(null).build();
        }
        Cart cart = cartOpt.get();
        List<CartItem> items = cartItemRepository.findAllByCartId(cart.getId());
        Map<String, Object> result = buildCartResponse(cart, items);
        return ApiResponse.builder().status(200).message("Success").data(result).build();
    }

    @Override
    public ApiResponse getAllCarts(Long userId, Long guestSessionId) {
        List<Cart> carts;
        if (userId != null) {
            carts = cartRepository.findAllByUserId(userId);
        } else if (guestSessionId != null) {
            carts = cartRepository.findAllByGuestSessionId(guestSessionId);
        } else {
            return ApiResponse.builder().status(400).message("userId or guestSessionId required").build();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Cart cart : carts) {
            List<CartItem> items = cartItemRepository.findAllByCartId(cart.getId());
            result.add(buildCartResponse(cart, items));
        }
        return ApiResponse.builder().status(200).message("Success").data(result).build();
    }

    @Override
    @Transactional
    public ApiResponse addToCart(AddToCartRequest request) {
        if (request.getShopId() == null || request.getItemId() == null) {
            return ApiResponse.builder().status(400).message("shopId and itemId are required").build();
        }

        // Find or create cart for this user + shop
        Optional<Cart> cartOpt = findCart(request.getUserId(), request.getGuestSessionId(), request.getShopId());
        Cart cart;
        if (cartOpt.isEmpty()) {
            cart = Cart.builder()
                    .userId(request.getUserId())
                    .guestSessionId(request.getGuestSessionId())
                    .shopId(request.getShopId())
                    .areaId(request.getAreaId() != null ? request.getAreaId() : 1L)
                    .subtotal(BigDecimal.ZERO)
                    .discountAmount(BigDecimal.ZERO)
                    .expiresAt(OffsetDateTime.now().plusHours(24))
                    .build();
            cart = cartRepository.save(cart);
        } else {
            cart = cartOpt.get();
        }

        // Calculate total price for new item
        BigDecimal optionsExtra = BigDecimal.ZERO;
        List<Map<String, Object>> opts = request.getSelectedOptions();
        if (opts != null) {
            for (Map<String, Object> opt : opts) {
                Object ep = opt.get("extra_price");
                if (ep != null) {
                    try {
                        optionsExtra = optionsExtra.add(new BigDecimal(ep.toString()));
                    } catch (Exception ignored) {}
                }
            }
        }

        BigDecimal unitPrice = request.getUnitPrice() != null ? request.getUnitPrice() : BigDecimal.ZERO;
        short qty = request.getQuantity() != null ? request.getQuantity() : 1;
        BigDecimal totalPrice = unitPrice.add(optionsExtra).multiply(BigDecimal.valueOf(qty));

        CartItem item = CartItem.builder()
                .cart(cart)
                .itemId(request.getItemId())
                .itemName(request.getItemName())
                .unitPrice(unitPrice)
                .quantity(qty)
                .selectedOptions(request.getSelectedOptions())
                .itemNote(request.getItemNote())
                .totalPrice(totalPrice)
                .build();

        cartItemRepository.save(item);

        // Recalculate cart subtotal
        recalculateSubtotal(cart);

        List<CartItem> allItems = cartItemRepository.findAllByCartId(cart.getId());
        Map<String, Object> result = buildCartResponse(cart, allItems);
        return ApiResponse.builder().status(200).message("Added to cart").data(result).build();
    }

    @Override
    @Transactional
    public ApiResponse updateCartItem(Long cartItemId, Long userId, Long guestSessionId, UpdateCartItemRequest request) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem not found"));

        Cart cart = cartItem.getCart();
        // Authorization check
        if (userId != null && !userId.equals(cart.getUserId())) {
            return ApiResponse.builder().status(403).message("Forbidden").build();
        }
        if (guestSessionId != null && !guestSessionId.equals(cart.getGuestSessionId())) {
            return ApiResponse.builder().status(403).message("Forbidden").build();
        }

        if (request.getQuantity() != null && request.getQuantity() <= 0) {
            // Remove item if quantity is 0
            cartItemRepository.delete(cartItem);
            recalculateSubtotal(cart);
            return ApiResponse.builder().status(200).message("Item removed").build();
        }

        if (request.getQuantity() != null) {
            cartItem.setQuantity(request.getQuantity());
        }
        if (request.getSelectedOptions() != null) {
            cartItem.setSelectedOptions(request.getSelectedOptions());
        }
        if (request.getItemNote() != null) {
            cartItem.setItemNote(request.getItemNote());
        }

        // Recalculate item total
        BigDecimal optionsExtra = BigDecimal.ZERO;
        List<Map<String, Object>> opts = cartItem.getSelectedOptions();
        if (opts != null) {
            for (Map<String, Object> opt : opts) {
                Object ep = opt.get("extra_price");
                if (ep != null) {
                    try { optionsExtra = optionsExtra.add(new BigDecimal(ep.toString())); } catch (Exception ignored) {}
                }
            }
        }
        cartItem.setTotalPrice(cartItem.getUnitPrice().add(optionsExtra).multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        cartItemRepository.save(cartItem);

        recalculateSubtotal(cart);

        List<CartItem> allItems = cartItemRepository.findAllByCartId(cart.getId());
        Map<String, Object> result = buildCartResponse(cart, allItems);
        return ApiResponse.builder().status(200).message("Updated").data(result).build();
    }

    @Override
    @Transactional
    public ApiResponse removeCartItem(Long cartItemId, Long userId, Long guestSessionId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem not found"));

        Cart cart = cartItem.getCart();
        cartItemRepository.delete(cartItem);
        recalculateSubtotal(cart);

        List<CartItem> allItems = cartItemRepository.findAllByCartId(cart.getId());
        Map<String, Object> result = buildCartResponse(cart, allItems);
        return ApiResponse.builder().status(200).message("Removed").data(result).build();
    }

    @Override
    @Transactional
    public ApiResponse clearCart(Long cartId, Long userId, Long guestSessionId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        cartItemRepository.deleteAllByCartId(cartId);
        cart.setSubtotal(BigDecimal.ZERO);
        cartRepository.save(cart);
        return ApiResponse.builder().status(200).message("Cart cleared").build();
    }

    // ---- Private helpers ----

    private Optional<Cart> findCart(Long userId, Long guestSessionId, Long shopId) {
        if (userId != null && shopId != null) {
            return cartRepository.findByUserIdAndShopId(userId, shopId);
        } else if (guestSessionId != null && shopId != null) {
            return cartRepository.findByGuestSessionIdAndShopId(guestSessionId, shopId);
        }
        return Optional.empty();
    }

    private void recalculateSubtotal(Cart cart) {
        List<CartItem> items = cartItemRepository.findAllByCartId(cart.getId());
        BigDecimal subtotal = items.stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setSubtotal(subtotal);
        cartRepository.save(cart);
    }

    private Map<String, Object> buildCartResponse(Cart cart, List<CartItem> items) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", cart.getId());
        result.put("shopId", cart.getShopId());
        result.put("userId", cart.getUserId());
        result.put("guestSessionId", cart.getGuestSessionId());
        result.put("subtotal", cart.getSubtotal());
        result.put("discountAmount", cart.getDiscountAmount());
        result.put("promotionCode", cart.getPromotionCode());

        List<Map<String, Object>> itemList = new ArrayList<>();
        for (CartItem ci : items) {
            Map<String, Object> itemMap = new LinkedHashMap<>();
            itemMap.put("id", ci.getId());
            itemMap.put("itemId", ci.getItemId());
            itemMap.put("itemName", ci.getItemName());
            itemMap.put("unitPrice", ci.getUnitPrice());
            itemMap.put("quantity", ci.getQuantity());
            itemMap.put("selectedOptions", ci.getSelectedOptions());
            itemMap.put("itemNote", ci.getItemNote());
            itemMap.put("totalPrice", ci.getTotalPrice());
            itemList.add(itemMap);
        }
        result.put("items", itemList);
        result.put("itemCount", items.stream().mapToInt(i -> i.getQuantity()).sum());
        return result;
    }
}
