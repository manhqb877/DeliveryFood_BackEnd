package com.fooddelivery.orders.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class AddToCartRequest {
    private Long shopId;
    private Long areaId;
    private Long itemId;
    private String itemName;
    private BigDecimal unitPrice;
    private Short quantity = 1;
    private List<Map<String, Object>> selectedOptions;
    private String itemNote;
    // For guest users
    private Long guestSessionId;
    // For logged-in users (can come from JWT, but also allow from body for simplicity)
    private Long userId;
}
