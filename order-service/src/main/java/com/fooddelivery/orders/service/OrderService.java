package com.fooddelivery.orders.service;

import com.fooddelivery.orders.dto.request.OrderRequest;
import com.fooddelivery.orders.dto.request.UpdateOrderStatusRequest;
import com.fooddelivery.orders.dto.response.OrderItemResponse;
import com.fooddelivery.orders.dto.response.OrderResponse;
import com.fooddelivery.orders.entity.*;
import com.fooddelivery.orders.enums.ActorType;
import com.fooddelivery.orders.enums.OrderStatus;
import com.fooddelivery.orders.enums.PaymentMethod;
import com.fooddelivery.orders.enums.PaymentStatus;
import com.fooddelivery.orders.enums.SagaStatus;
import com.fooddelivery.orders.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final RestTemplate restTemplate;
    private final com.fooddelivery.orders.kafka.OrderEventProducer orderEventProducer;

    @Transactional
    public OrderResponse placeOrder(OrderRequest request, Long userId, Long guestSessionId) {
        // 1. Idempotency Check: prevent duplicate orders and double charging on retries or double clicks
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().trim().isEmpty()) {
            Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(request.getIdempotencyKey().trim());
            if (existingOrder.isPresent()) {
                log.info("Idempotent duplicate order request detected for key: {}. Returning existing order #{}",
                        request.getIdempotencyKey(), existingOrder.get().getOrderCode());
                List<OrderItem> items = orderItemRepository.findByOrderId(existingOrder.get().getId());
                return mapToResponse(existingOrder.get(), items);
            }
        }

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

        // 2. Atomic Inventory Check & Deduction against Race Conditions
        List<Map<String, Object>> deductItems = cartItems.stream().map(ci -> {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("itemId", ci.getItemId());
            itemMap.put("quantity", (int) ci.getQuantity());
            return itemMap;
        }).collect(Collectors.toList());

        Map<String, Object> deductPayload = new HashMap<>();
        deductPayload.put("items", deductItems);

        try {
            String coreServiceUrl = System.getenv().getOrDefault("CORE_SERVICE_URL", "http://localhost:8082");
            restTemplate.postForEntity(coreServiceUrl + "/core/items/deduct-stock", deductPayload, Void.class);
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            log.error("Failed to deduct inventory: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new RuntimeException("Món ăn trong giỏ đã hết hàng hoặc không đủ số lượng tồn kho để đặt!");
        } catch (Exception ex) {
            log.warn("Core service inventory check warning: {}", ex.getMessage());
        }

        String shortUuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String orderCode = "ORD-" + (System.currentTimeMillis() / 1000) + "-" + shortUuid.substring(0, 4);

        BigDecimal subtotal = cartItems.stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal deliveryFee = new BigDecimal("15000");

        // 3. Discount calculation and promotion verification
        BigDecimal discount = request.getDiscountAmount() != null && request.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0
                ? request.getDiscountAmount()
                : (cart.getDiscountAmount() != null ? cart.getDiscountAmount() : BigDecimal.ZERO);

        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }

        BigDecimal totalAmount = subtotal.add(deliveryFee).subtract(discount);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }

        String promoCode = request.getPromotionCode() != null && !request.getPromotionCode().isBlank()
                ? request.getPromotionCode()
                : cart.getPromotionCode();
        Long promoId = request.getPromotionId();

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
                .promotionId(promoId)
                .promotionCode(promoCode)
                .paymentMethod(PaymentMethod.valueOf(request.getPaymentMethod()))
                .paymentStatus(PaymentStatus.PENDING)
                .orderStatus(OrderStatus.PLACED)
                .orderNote(request.getOrderNote())
                .idempotencyKey(request.getIdempotencyKey())
                .sagaStatus(SagaStatus.COMPLETED)
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
                .note("Khách hàng đặt đơn thành công (COD)")
                .build();
        statusHistoryRepository.save(history);

        log.info("Order created successfully: id={}, code={}, totalAmount={}", savedOrder.getId(), savedOrder.getOrderCode(), savedOrder.getTotalAmount());

        cartItemRepository.deleteAll(cartItems);
        cartRepository.delete(cart);

        // Publish event to Kafka for Notification Service
        orderEventProducer.publishOrderCreated(savedOrder, orderItems);

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

    public List<OrderResponse> getShopOrders(Long shopId) {
        List<Order> orders = orderRepository.findByShopIdOrderByPlacedAtDesc(shopId);
        return orders.stream().map(order -> {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            return mapToResponse(order, items);
        }).collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        OrderStatus oldStatus = order.getOrderStatus();
        OrderStatus newStatus = request.getStatus();
        OffsetDateTime now = OffsetDateTime.now();

        order.setOrderStatus(newStatus);

        if (newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.PREPARING) {
            if (order.getConfirmedAt() == null) {
                order.setConfirmedAt(now);
            }
        } else if (newStatus == OrderStatus.READY_FOR_PICKUP) {
            order.setReadyAt(now);
        } else if (newStatus == OrderStatus.DELIVERED) {
            order.setDeliveredAt(now);
        } else if (newStatus == OrderStatus.COMPLETED) {
            order.setCompletedAt(now);
        } else if (newStatus == OrderStatus.CANCELLED) {
            order.setCancelledAt(now);
            if (request.getCancelReason() != null) {
                order.setCancelReason(request.getCancelReason());
            }
            if (request.getActorType() != null) {
                order.setCancelledBy(request.getActorType());
            } else {
                order.setCancelledBy("SHOP");
            }
        }

        Order updatedOrder = orderRepository.save(order);

        ActorType actor = ActorType.SHOP_MANAGER;
        if ("CUSTOMER".equalsIgnoreCase(request.getActorType())) actor = ActorType.CUSTOMER;
        else if ("SHIPPER".equalsIgnoreCase(request.getActorType())) actor = ActorType.SHIPPER;
        else if ("ADMIN".equalsIgnoreCase(request.getActorType())) actor = ActorType.ADMIN;

        OrderStatusHistory history = OrderStatusHistory.builder()
                .orderId(updatedOrder.getId())
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .actorType(actor)
                .note(request.getCancelReason() != null ? "Lý do: " + request.getCancelReason() : "Trạng thái đổi từ " + oldStatus + " sang " + newStatus)
                .build();
        statusHistoryRepository.save(history);

        log.info("Order ID: {} status updated from {} to {}", orderId, oldStatus, newStatus);
        
        // Publish status update event to Kafka
        orderEventProducer.publishOrderStatusChanged(updatedOrder, oldStatus != null ? oldStatus.name() : null, newStatus.name());

        List<OrderItem> items = orderItemRepository.findByOrderId(updatedOrder.getId());
        return mapToResponse(updatedOrder, items);
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
                .cancelReason(order.getCancelReason())
                .cancelledBy(order.getCancelledBy())
                .placedAt(order.getPlacedAt())
                .confirmedAt(order.getConfirmedAt())
                .readyAt(order.getReadyAt())
                .completedAt(order.getCompletedAt())
                .cancelledAt(order.getCancelledAt())
                .items(itemResponses)
                .build();
    }
}
