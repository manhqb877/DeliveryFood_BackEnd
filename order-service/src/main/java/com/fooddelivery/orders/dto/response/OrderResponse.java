package com.fooddelivery.orders.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class OrderResponse {
    private Long id;
    private String orderCode;
    private Long userId;
    private Long guestSessionId;
    private Long shopId;
    private String shopName;
    private Map<String, Object> deliveryAddress;
    
    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    
    private String paymentMethod;
    private String paymentStatus;
    private String orderStatus;
    
    private String orderNote;
    
    private OffsetDateTime placedAt;
    private OffsetDateTime confirmedAt;
    
    private List<OrderItemResponse> items;
}
