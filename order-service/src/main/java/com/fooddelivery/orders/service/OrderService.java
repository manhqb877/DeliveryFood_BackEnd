package com.fooddelivery.orders.service;

import com.fooddelivery.orders.dto.request.OrderRequest;
import com.fooddelivery.orders.dto.request.UpdateOrderStatusRequest;
import com.fooddelivery.orders.dto.response.OrderItemResponse;
import com.fooddelivery.orders.dto.response.OrderResponse;
import com.fooddelivery.orders.dto.response.OrderStatusHistoryResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
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
    private final com.fooddelivery.orders.client.CoreServiceClient coreServiceClient;
    private final com.fooddelivery.orders.kafka.OrderEventProducer orderEventProducer;
    private final CodRemittanceService codRemittanceService;

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

        List<Long> cartIds = new java.util.ArrayList<>();
        if (request.getCartIds() != null && !request.getCartIds().isEmpty()) {
            cartIds.addAll(request.getCartIds());
        } else if (request.getCartId() != null) {
            cartIds.add(request.getCartId());
        }

        if (cartIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart ID is required");
        }

        List<Cart> carts = cartRepository.findAllById(cartIds);
        if (carts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found");
        }

        for (Cart c : carts) {
            if (userId != null && !userId.equals(c.getUserId())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized: User does not own this cart");
            }
            if (userId == null && c.getUserId() != null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized: Session expired, please log in again");
            }
            if (userId == null && guestSessionId != null && !guestSessionId.equals(c.getGuestSessionId())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized: Guest session mismatch");
            }
        }

        Cart primaryCart = carts.get(0);
        Long orderUserId = primaryCart.getUserId();
        Long orderGuestSessionId = primaryCart.getGuestSessionId();

        List<CartItem> cartItems = new java.util.ArrayList<>();
        for (Cart c : carts) {
            cartItems.addAll(cartItemRepository.findAllByCartId(c.getId()));
        }
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        // Kiểm tra tất cả các quán có đang mở cửa không
        for (Cart c : carts) {
            try {
                Map<String, Object> shop = coreServiceClient.getShopDetails(c.getShopId());
                if (shop != null) {
                    Boolean isOpen = (Boolean) shop.get("isOpen");
                    Boolean isAcceptingOrders = (Boolean) shop.get("isAcceptingOrders");
                    if (Boolean.FALSE.equals(isOpen) || Boolean.FALSE.equals(isAcceptingOrders)) {
                        String sName = (String) shop.get("shopName");
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quán " + (sName != null ? sName : "") + " hiện đang đóng cửa hoặc tạm ngừng nhận đơn!");
                    }
                }
            } catch (ResponseStatusException rse) {
                throw rse;
            } catch (Exception ex) {
                log.warn("Check shop open status warning: {}", ex.getMessage());
            }
        }

        // Atomic Inventory Check & Deduction
        List<Map<String, Object>> deductItems = cartItems.stream().map(ci -> {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("itemId", ci.getItemId());
            itemMap.put("quantity", (int) ci.getQuantity());
            return itemMap;
        }).collect(Collectors.toList());

        Map<String, Object> deductPayload = new HashMap<>();
        deductPayload.put("items", deductItems);

        try {
            coreServiceClient.deductStock(deductPayload);
        } catch (feign.FeignException ex) {
            log.error("Failed to deduct inventory via CoreServiceClient: status={}, content={}", ex.status(), ex.contentUTF8());
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

        // Discount calculation
        BigDecimal discount = request.getDiscountAmount() != null && request.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0
                ? request.getDiscountAmount()
                : (primaryCart.getDiscountAmount() != null ? primaryCart.getDiscountAmount() : BigDecimal.ZERO);

        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }

        BigDecimal totalAmount = subtotal.add(deliveryFee).subtract(discount);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }

        String promoCode = request.getPromotionCode() != null && !request.getPromotionCode().isBlank()
                ? request.getPromotionCode()
                : primaryCart.getPromotionCode();
        Long promoId = request.getPromotionId();

        // Xử lý shopName và shop2 nếu đa quán
        Long shop1Id = primaryCart.getShopId();
        String shop1Name = "Shop #" + shop1Id;
        try {
            Map<String, Object> s1 = coreServiceClient.getShopDetails(shop1Id);
            if (s1 != null && s1.get("shopName") != null) {
                shop1Name = (String) s1.get("shopName");
            }
        } catch (Exception ex) {
            log.warn("Could not fetch shop1 details: {}", ex.getMessage());
        }

        Long shop2Id = null;
        String shop2Name = null;
        Double pickup2Lat = null;
        Double pickup2Lng = null;
        String pickup2Address = null;

        if (carts.size() > 1) {
            Cart secondCart = carts.get(1);
            shop2Id = secondCart.getShopId();
            shop2Name = "Shop #" + shop2Id;
            try {
                Map<String, Object> s2 = coreServiceClient.getShopDetails(shop2Id);
                if (s2 != null) {
                    if (s2.get("shopName") != null) {
                        shop2Name = (String) s2.get("shopName");
                    }
                    if (s2.get("latitude") != null) pickup2Lat = Double.valueOf(s2.get("latitude").toString());
                    else if (s2.get("shopLat") != null) pickup2Lat = Double.valueOf(s2.get("shopLat").toString());
                    else if (s2.get("lat") != null) pickup2Lat = Double.valueOf(s2.get("lat").toString());

                    if (s2.get("longitude") != null) pickup2Lng = Double.valueOf(s2.get("longitude").toString());
                    else if (s2.get("shopLng") != null) pickup2Lng = Double.valueOf(s2.get("shopLng").toString());
                    else if (s2.get("lng") != null) pickup2Lng = Double.valueOf(s2.get("lng").toString());

                    if (s2.get("locationDetail") != null) pickup2Address = (String) s2.get("locationDetail");
                    else if (s2.get("address") != null) pickup2Address = (String) s2.get("address");
                }
            } catch (Exception ex) {
                log.warn("Could not fetch shop2 details: {}", ex.getMessage());
            }
        }

        String finalShopName = shop2Name != null ? (shop1Name + " & " + shop2Name) : shop1Name;

        Order order = Order.builder()
                .orderCode(orderCode)
                .userId(orderUserId)
                .guestSessionId(orderGuestSessionId)
                .shopId(shop1Id)
                .shopName(finalShopName)
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
                .note("Khách hàng đặt đơn thành công (" + request.getPaymentMethod() + ")")
                .build();
        statusHistoryRepository.save(history);

        log.info("Order created successfully: id={}, code={}, totalAmount={}, shops={}",
                savedOrder.getId(), savedOrder.getOrderCode(), savedOrder.getTotalAmount(), finalShopName);

        cartItemRepository.deleteAll(cartItems);
        cartRepository.deleteAll(carts);

        // Publish event to Kafka for Notification & Tracking Service
        orderEventProducer.publishOrderCreated(savedOrder, orderItems, shop2Id, shop2Name, pickup2Lat, pickup2Lng, pickup2Address);

        // Redeem promotion in Core Service if order used a voucher
        if (savedOrder.getPromotionId() != null || (savedOrder.getPromotionCode() != null && !savedOrder.getPromotionCode().isBlank())) {
            try {
                Map<String, Object> promoPayload = new HashMap<>();
                promoPayload.put("promotionId", savedOrder.getPromotionId());
                promoPayload.put("promoCode", savedOrder.getPromotionCode());
                promoPayload.put("orderId", savedOrder.getId());
                promoPayload.put("userId", orderUserId);
                promoPayload.put("guestSessionId", orderGuestSessionId);
                promoPayload.put("orderAmount", savedOrder.getSubtotal());
                promoPayload.put("discountAmount", savedOrder.getDiscountAmount());
                promoPayload.put("idempotencyKey", savedOrder.getIdempotencyKey());
                coreServiceClient.redeemPromotion(promoPayload);
                log.info("Redeemed promotion {} for order ID: {}", savedOrder.getPromotionCode(), savedOrder.getId());
            } catch (Exception ex) {
                log.warn("Could not redeem promotion via coreServiceClient: {}", ex.getMessage());
            }
        }

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

        if (orders.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
        List<OrderItem> allItems = orderItemRepository.findByOrderIdIn(orderIds);
        Map<Long, List<OrderItem>> itemsByOrderId = allItems.stream()
                .collect(Collectors.groupingBy(item -> item.getOrder().getId()));

        return orders.stream().map(order -> {
            List<OrderItem> items = itemsByOrderId.getOrDefault(order.getId(), java.util.Collections.emptyList());
            return mapToResponse(order, items);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderStatusHistoryResponse> getOrderHistory(Long orderId) {
        return statusHistoryRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(history -> OrderStatusHistoryResponse.builder()
                        .id(history.getId())
                        .orderId(history.getOrderId())
                        .oldStatus(history.getOldStatus())
                        .newStatus(history.getNewStatus())
                        .actorType(history.getActorType())
                        .actorId(history.getActorId())
                        .note(history.getNote())
                        .createdAt(history.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getShopOrders(Long shopId) {
        List<Order> orders = orderRepository.findByShopIdOrderByPlacedAtDesc(shopId);
        if (orders.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
        List<OrderItem> allItems = orderItemRepository.findByOrderIdIn(orderIds);
        Map<Long, List<OrderItem>> itemsByOrderId = allItems.stream()
                .collect(Collectors.groupingBy(item -> item.getOrder().getId()));

        return orders.stream().map(order -> {
            List<OrderItem> items = itemsByOrderId.getOrDefault(order.getId(), java.util.Collections.emptyList());
            return mapToResponse(order, items);
        }).collect(Collectors.toList());
    }

    public List<OrderResponse> getAllOrders() {
        List<Order> orders = orderRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "placedAt"));
        if (orders.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
        List<OrderItem> allItems = orderItemRepository.findByOrderIdIn(orderIds);
        Map<Long, List<OrderItem>> itemsByOrderId = allItems.stream()
                .collect(Collectors.groupingBy(item -> item.getOrder().getId()));

        return orders.stream().map(order -> {
            List<OrderItem> items = itemsByOrderId.getOrDefault(order.getId(), java.util.Collections.emptyList());
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
            codRemittanceService.generateRemittanceIfApplicable(order);
            try {
                String trackingUrl = System.getenv().getOrDefault("TRACKING_SERVICE_URL", "http://localhost:8084");
                java.util.Map<String, Object> deliveryInfo = restTemplate.getForObject(trackingUrl + "/tracking/deliveries/order/" + order.getId(), java.util.Map.class);
                if (deliveryInfo != null && deliveryInfo.get("shipperId") != null) {
                    Long shipperId = Long.valueOf(deliveryInfo.get("shipperId").toString());
                    String authUrl = System.getenv().getOrDefault("AUTH_SERVICE_URL", "http://localhost:8081");
                        restTemplate.postForLocation(authUrl + "/auth/shippers/" + shipperId + "/increment-delivery", null);
                        log.info("Incremented total deliveries for shipper {}", shipperId);
                    }
            } catch (Exception e) {
                log.warn("Could not increment deliveries for order {}: {}", order.getId(), e.getMessage());
            }
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
                .promotionCode(order.getPromotionCode())
                .promotionId(order.getPromotionId())
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

    @Transactional
    public OrderResponse markOrderAsPaid(Long orderId, String transactionId, String paymentGateway) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        order.setPaymentStatus(PaymentStatus.PAID);
        Order updatedOrder = orderRepository.save(order);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .orderId(updatedOrder.getId())
                .newStatus(updatedOrder.getOrderStatus())
                .actorType(ActorType.SYSTEM)
                .note("Đã thanh toán thành công qua " + (paymentGateway != null ? paymentGateway : "SEPAY")
                        + (transactionId != null ? " (Mã GD: " + transactionId + ")" : ""))
                .build();
        statusHistoryRepository.save(history);

        log.info("Order {} marked as PAID via {}", orderId, paymentGateway);

        List<OrderItem> items = orderItemRepository.findByOrderId(updatedOrder.getId());
        return mapToResponse(updatedOrder, items);
    }
}
