package com.fooddelivery.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentQrRequest {
    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotNull(message = "Order code is required")
    private String orderCode;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    private Long userId;
}
