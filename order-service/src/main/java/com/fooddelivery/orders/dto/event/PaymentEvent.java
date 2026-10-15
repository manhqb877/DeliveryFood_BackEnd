package com.fooddelivery.orders.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentEvent implements Serializable {
    private String eventId;
    private String eventType; // PAYMENT_COMPLETED, PAYMENT_FAILED
    private Long orderId;
    private String orderCode;
    private Long userId;
    private String gatewayTransactionId;
    private String paymentGateway;
    private BigDecimal amount;
    private String status; // SUCCESS, FAILED
    private Instant timestamp;
}
