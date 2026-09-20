package com.fooddelivery.core.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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
public class ValidatePromotionRequest {

    @NotBlank(message = "Mã voucher không được để trống")
    private String code;

    private Long shopId;

    private Long userId;

    private Long guestSessionId;

    @NotNull(message = "Giá trị đơn hàng không được để trống")
    @DecimalMin(value = "0.0", message = "Giá trị đơn hàng không hợp lệ")
    private BigDecimal orderAmount;
}
