package com.fooddelivery.orders.service;

import com.fooddelivery.orders.dto.request.OrderRequest;
import com.fooddelivery.orders.dto.response.OrderItemResponse;
import com.fooddelivery.orders.dto.response.OrderResponse;
import com.fooddelivery.orders.entity.*;
import com.fooddelivery.orders.enums.ActorType;
import com.fooddelivery.orders.enums.OrderStatus;
import com.fooddelivery.orders.enums.PaymentMethod;
import com.fooddelivery.orders.enums.PaymentStatus;
import com.fooddelivery.orders.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    @Transactional
    public OrderResponse placeOrder(OrderRequest request, Long userId, Long guestSessionId) {
        Cart cart = cartRepository.findById(request.getCartId())
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        if (userId != null && !userId.equals(cart.getUserId())) {
            throw new RuntimeException("Unauthorized");
        }
        if (userId == null && guestSessionId != null && !guestSessionId.equals(cart.getGuestSessionId())) {
            throw new RuntimeException("Unauthorized");
        }

        // Always use the cart's owner to prevent orphaned orders if API Gateway misses headers
        Long orderUserId = cart.getUserId();
        Long orderGuestSessionId = cart.getGuestSessionId();

        List<CartItem> cartItems = cartItemRepository.findAllByCartId(cart.getId());
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        String shortUuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String orderCode = "ORD-" + (System.currentTimeMillis() / 1000) + "-" + shortUuid.substring(0, 4);

        BigDecimal subtotal = cartItems.stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal deliveryFee = new BigDecimal("15000");
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.add(deliveryFee).subtract(discount);

        Order order = Order.builder()
                .orderCode(orderCode)
                .userId(orderUserId)
                .guestSessionId(orderGuestSessionId)
                .shopId(cart.getShopId())
                .shopName("Shop #" + cart.getShopId())
                .areaId(1L)
                .deliveryAddress(request.getDeliveryAddress())
                .subtotal(subtotal)
                .deliveryFee(deliveryFee)
                .discountAmount(discount)
                .totalAmount(totalAmount)
                .paymentMethod(PaymentMethod.valueOf(request.getPaymentMethod()))
                .paymentStatus(PaymentStatus.PENDING)
                .orderStatus(OrderStatus.PLACED)
                .orderNote(request.getOrderNote())
                .placedAt(OffsetDateTime.now())
                .build();

        Order savedOrder = orderRepository.save(order);

        List<OrderItem> orderItems = cartItems.stream().map(ci -> 
            OrderItem.builder()
                    .order(savedOrder)
                    .itemId(ci.getItemId())
                    .itemName(ci.getItemName())
                    .quantity(ci.getQuantity().shortValue())
                    .unitPrice(ci.getUnitPrice())
                    .totalPrice(ci.getTotalPrice())
                    .selectedOptions(ci.getSelectedOptions())
                    .itemNote(ci.getItemNote())
                    .build()
        ).collect(Collectors.toList());
        orderItemRepository.saveAll(orderItems);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .orderId(savedOrder.getId())
                .newStatus(OrderStatus.PLACED)
                .actorType(userId != null ? ActorType.CUSTOMER : ActorType.SYSTEM)
                .actorId(userId)
                .note("Khách hàng đặt đơn")
                .build();
        statusHistoryRepository.save(history);

        log.info("TODO: Publish event to Kafka for payment saga processing. OrderID: {}", savedOrder.getId());

        cartItemRepository.deleteAll(cartItems);
        cartRepository.delete(cart);

        return mapToResponse(savedOrder, orderItems);
    }

    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        return mapToResponse(order, items);
    }

    public OrderResponse getOrderByCode(String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        return mapToResponse(order, items);
    }

    public List<OrderResponse> getUserOrders(Long userId, Long guestSessionId) {
        List<Order> orders;
        if (userId != null) {
            orders = orderRepository.findByUserIdOrderByPlacedAtDesc(userId);
        } else if (guestSessionId != null) {
            orders = orderRepository.findByGuestSessionIdOrderByPlacedAtDesc(guestSessionId);
        } else {
            throw new IllegalArgumentException("Must provide either userId or guestSessionId");
        }

        return orders.stream().map(order -> {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            return mapToResponse(order, items);
        }).collect(Collectors.toList());
    }

    private OrderResponse mapToResponse(Order order, List<OrderItem> items) {
        List<OrderItemResponse> itemResponses = items.stream().map(i -> 
            OrderItemResponse.builder()
                    .id(i.getId())
                    .itemId(i.getItemId())
                    .itemName(i.getItemName())
                    .quantity(i.getQuantity().intValue())
                    .unitPrice(i.getUnitPrice())
                    .totalPrice(i.getTotalPrice())
                    .selectedOptions(i.getSelectedOptions())
                    .itemNote(i.getItemNote())
                    .build()
        ).collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .userId(order.getUserId())
                .guestSessionId(order.getGuestSessionId())
                .shopId(order.getShopId())
                .shopName(order.getShopName())
                .deliveryAddress(order.getDeliveryAddress())
                .subtotal(order.getSubtotal())
                .deliveryFee(order.getDeliveryFee())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .paymentMethod(order.getPaymentMethod().name())
                .paymentStatus(order.getPaymentStatus().name())
                .orderStatus(order.getOrderStatus().name())
                .orderNote(order.getOrderNote())
                .placedAt(order.getPlacedAt())
                .confirmedAt(order.getConfirmedAt())
                .items(itemResponses)
                .build();
    }
}
