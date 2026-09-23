package com.fooddelivery.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserOrderResponse {
    private Long id;
    private String orderCode;
    private Long userId;
    private Long shopId;
    private String shopName;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private String paymentStatus;
    private String orderStatus;
    private OffsetDateTime placedAt;
    private OffsetDateTime completedAt;
    private OffsetDateTime cancelledAt;
}
