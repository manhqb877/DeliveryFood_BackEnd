package com.fooddelivery.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOrderStatsResponse {
    private int totalOrders;
    private BigDecimal totalSpent;
    private int completedOrders;
    private int cancelledOrders;
    private List<UserOrderResponse> recentOrders;
}
