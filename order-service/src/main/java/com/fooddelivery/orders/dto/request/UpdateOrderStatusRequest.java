package com.fooddelivery.orders.dto.request;

import com.fooddelivery.orders.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrderStatusRequest {

    @NotNull(message = "Status cannot be null")
    private OrderStatus status;

    private String cancelReason;

    private String actorType; // SHOP, CUSTOMER, SHIPPER, ADMIN
}
