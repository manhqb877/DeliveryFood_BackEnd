package com.fooddelivery.orders.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class OrderRequest {
    private Long cartId;

    private java.util.List<Long> cartIds;

    @NotNull(message = "Delivery address is required")
    private Map<String, Object> deliveryAddress;

    @NotNull(message = "Payment method is required")
    private String paymentMethod; // e.g. COD, ONLINE, WALLET

    private String orderNote;

    private String idempotencyKey;

    private String promotionCode;

    private Long promotionId;

    private BigDecimal discountAmount;
}

