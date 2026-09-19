package com.fooddelivery.orders.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class OrderItemResponse {
    private Long id;
    private Long itemId;
    private String itemName;
    private String itemImage;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private List<Map<String, Object>> selectedOptions;
    private String itemNote;
}
