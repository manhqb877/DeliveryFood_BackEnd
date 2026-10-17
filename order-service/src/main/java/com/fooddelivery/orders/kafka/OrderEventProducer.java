package com.fooddelivery.orders.kafka;

import com.fooddelivery.orders.dto.event.OrderEvent;
import com.fooddelivery.orders.entity.Order;
import com.fooddelivery.orders.entity.OrderItem;
import com.fooddelivery.orders.enums.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    public static final String TOPIC_ORDER_EVENTS = "order-events";

    public void publishOrderCreated(Order order, List<OrderItem> orderItems) {
        publishOrderCreated(order, orderItems, null, null, null, null, null);
    }

    public void publishOrderCreated(Order order, List<OrderItem> orderItems,
                                    Long shop2Id, String shop2Name,
                                    Double pickup2Lat, Double pickup2Lng, String pickup2Address) {
        try {
            List<OrderEvent.OrderItemSummary> itemSummaries = (orderItems != null)
                    ? orderItems.stream().map(item -> OrderEvent.OrderItemSummary.builder()
                            .itemId(item.getItemId())
                            .itemName(item.getItemName())
                            .quantity(item.getQuantity() != null ? item.getQuantity().intValue() : 1)
                            .price(item.getUnitPrice())
                            .build()).collect(Collectors.toList())
                    : Collections.emptyList();

            String customerName = extractField(order.getDeliveryAddress(), "recipientName");
            String customerPhone = extractFieldMulti(order.getDeliveryAddress(), "recipientPhone", "phoneNumber");
            String addressText = extractFieldMulti(order.getDeliveryAddress(), "fullAddress", "address");
            Double lat = extractDoubleMulti(order.getDeliveryAddress(), "latitude", "lat");
            Double lng = extractDoubleMulti(order.getDeliveryAddress(), "longitude", "lng");

            OrderEvent event = OrderEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("ORDER_CREATED")
                    .orderId(order.getId())
                    .orderCode(order.getOrderCode())
                    .shopId(order.getShopId())
                    .shop2Id(shop2Id)
                    .shop2Name(shop2Name)
                    .pickup2Lat(pickup2Lat)
                    .pickup2Lng(pickup2Lng)
                    .pickup2Address(pickup2Address)
                    .userId(order.getUserId())
                    .guestSessionId(order.getGuestSessionId())
                    .customerName(customerName)
                    .customerPhone(customerPhone)
                    .deliveryAddress(addressText)
                    .deliveryLat(lat)
                    .deliveryLng(lng)
                    .totalAmount(order.getTotalAmount())
                    .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : "COD")
                    .orderStatus(order.getOrderStatus() != null ? order.getOrderStatus().name() : "PLACED")
                    .items(itemSummaries)
                    .timestamp(Instant.now())
                    .build();

            log.info("Publishing ORDER_CREATED event for order #{} (id={}) to Kafka topic '{}'",
                    order.getOrderCode(), order.getId(), TOPIC_ORDER_EVENTS);

            kafkaTemplate.send(TOPIC_ORDER_EVENTS, String.valueOf(order.getId()), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Successfully published ORDER_CREATED event for order #{} to partition {} offset {}",
                                    order.getOrderCode(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.warn("Failed to publish ORDER_CREATED event to Kafka: {}", ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.warn("Could not dispatch ORDER_CREATED event to Kafka broker: {}", e.getMessage());
        }
    }

    public void publishOrderStatusChanged(Order order, String oldStatus, String newStatus) {
        boolean allConfirmed = !OrderStatus.PLACED.name().equals(newStatus) && !OrderStatus.CANCELLED.name().equals(newStatus);
        publishOrderStatusChanged(order, oldStatus, newStatus, allConfirmed);
    }

    public void publishOrderStatusChanged(Order order, String oldStatus, String newStatus, boolean allConfirmed) {
        try {
            String customerName = extractField(order.getDeliveryAddress(), "recipientName");
            String customerPhone = extractFieldMulti(order.getDeliveryAddress(), "recipientPhone", "phoneNumber");
            String addressText = extractFieldMulti(order.getDeliveryAddress(), "fullAddress", "address");
            Double lat = extractDoubleMulti(order.getDeliveryAddress(), "latitude", "lat");
            Double lng = extractDoubleMulti(order.getDeliveryAddress(), "longitude", "lng");

            OrderEvent event = OrderEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("ORDER_STATUS_CHANGED")
                    .orderId(order.getId())
                    .orderCode(order.getOrderCode())
                    .shopId(order.getShopId())
                    .shop2Id(order.getShop2Id())
                    .shop2Name(order.getShop2Name())
                    .shop1Confirmed(order.getShop1Confirmed())
                    .shop2Confirmed(order.getShop2Confirmed())
                    .allConfirmed(allConfirmed)
                    .userId(order.getUserId())
                    .guestSessionId(order.getGuestSessionId())
                    .customerName(customerName)
                    .customerPhone(customerPhone)
                    .deliveryAddress(addressText)
                    .deliveryLat(lat)
                    .deliveryLng(lng)
                    .totalAmount(order.getTotalAmount())
                    .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : "COD")
                    .orderStatus(newStatus)
                    .cancelReason(order.getCancelReason())
                    .timestamp(Instant.now())
                    .build();

            log.info("Publishing ORDER_STATUS_CHANGED ({} -> {}) for order #{} (allConfirmed={}) to Kafka topic '{}'",
                    oldStatus, newStatus, order.getOrderCode(), allConfirmed, TOPIC_ORDER_EVENTS);

            kafkaTemplate.send(TOPIC_ORDER_EVENTS, String.valueOf(order.getId()), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Successfully published ORDER_STATUS_CHANGED event for order #{}", order.getOrderCode());
                        } else {
                            log.warn("Failed to publish ORDER_STATUS_CHANGED event to Kafka: {}", ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.warn("Could not dispatch ORDER_STATUS_CHANGED event to Kafka broker: {}", e.getMessage());
        }
    }

    private String extractField(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private String extractFieldMulti(Map<String, Object> map, String... keys) {
        if (map == null) return null;
        for (String key : keys) {
            Object val = map.get(key);
            if (val != null && !val.toString().isBlank()) return val.toString();
        }
        return null;
    }

    private Double extractDoubleMulti(Map<String, Object> map, String... keys) {
        if (map == null) return null;
        for (String key : keys) {
            Object val = map.get(key);
            if (val != null) {
                try {
                    return Double.parseDouble(val.toString());
                } catch (NumberFormatException e) {
                    // Ignore and try next key
                }
            }
        }
        return null;
    }

    private String extractFieldOld(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }
}
