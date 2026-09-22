package com.fooddelivery.tracking.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {
    private String eventId;
    private String eventType;
    private Long orderId;
    private String orderCode;
    private Long shopId;
    private Long userId;
    private String guestSessionId;
    
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
    public static class OrderItemSummary {
        private Long itemId;
        private String itemName;
        private Integer quantity;
        private BigDecimal price;
    }
}
