package com.fooddelivery.payment.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RemitCodRequest {
    private Long ownerId;
    private BigDecimal amount;
    private Long orderId;
}
