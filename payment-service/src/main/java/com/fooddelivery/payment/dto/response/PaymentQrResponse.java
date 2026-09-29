package com.fooddelivery.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentQrResponse {
    private Long orderId;
    private String orderCode;
    private BigDecimal amount;
    private String bankName;
    private String accountNumber;
    private String accountHolder;
    private String paymentDescription;
    private String qrUrl;
    private String paymentStatus; // PENDING, SUCCESS
}
