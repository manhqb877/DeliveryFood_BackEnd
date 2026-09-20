package com.fooddelivery.orders.kafka;

import com.fooddelivery.orders.dto.event.OrderEvent;
import com.fooddelivery.orders.entity.Order;
import com.fooddelivery.orders.entity.OrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
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
            String customerPhone = extractField(order.getDeliveryAddress(), "phoneNumber");
            String addressText = extractField(order.getDeliveryAddress(), "address");

            OrderEvent event = OrderEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("ORDER_CREATED")
                    .orderId(order.getId())
                    .orderCode(order.getOrderCode())
                    .shopId(order.getShopId())
                    .userId(order.getUserId())
                    .guestSessionId(order.getGuestSessionId())
                    .customerName(customerName)
                    .customerPhone(customerPhone)
                    .deliveryAddress(addressText)
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
        try {
            String customerName = extractField(order.getDeliveryAddress(), "recipientName");
            String customerPhone = extractField(order.getDeliveryAddress(), "phoneNumber");
            String addressText = extractField(order.getDeliveryAddress(), "address");

            OrderEvent event = OrderEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("ORDER_STATUS_CHANGED")
                    .orderId(order.getId())
                    .orderCode(order.getOrderCode())
                    .shopId(order.getShopId())
                    .userId(order.getUserId())
                    .guestSessionId(order.getGuestSessionId())
                    .customerName(customerName)
                    .customerPhone(customerPhone)
                    .deliveryAddress(addressText)
                    .totalAmount(order.getTotalAmount())
                    .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : "COD")
                    .orderStatus(newStatus)
                    .cancelReason(order.getCancelReason())
                    .timestamp(Instant.now())
                    .build();

            log.info("Publishing ORDER_STATUS_CHANGED ({} -> {}) for order #{} to Kafka topic '{}'",
                    oldStatus, newStatus, order.getOrderCode(), TOPIC_ORDER_EVENTS);

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
}
