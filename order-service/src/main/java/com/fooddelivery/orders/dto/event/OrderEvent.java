package com.fooddelivery.orders.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent implements Serializable {
    private String eventId;
    private String eventType; // "ORDER_CREATED", "ORDER_STATUS_CHANGED"
    private Long orderId;
    private String orderCode;
    private Long shopId;
    private Long userId;
    private Long guestSessionId;
    private String customerName;
    private String customerPhone;
    private String deliveryAddress;
    private Double deliveryLat;
    private Double deliveryLng;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private String orderStatus;
    private String cancelReason;
    private List<OrderItemSummary> items;
    private Instant timestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemSummary implements Serializable {
        private Long itemId;
        private String itemName;
        private Integer quantity;
        private BigDecimal price;
    }
}
