package com.fooddelivery.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApproveShopRequest {

    @NotNull(message = "Approval decision is required")
    private Boolean approved;

    private String rejectionReason;

    private BigDecimal commissionRate;
}
