package com.fooddelivery.core.dto.request;

import com.fooddelivery.core.enums.ApplicableTo;
import com.fooddelivery.core.enums.PromoType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePromotionRequest {

    @NotBlank(message = "Mã khuyến mãi không được để trống")
    private String code;

    @NotNull(message = "Loại khuyến mãi không được để trống")
    private PromoType promoType;

    @NotNull(message = "Giá trị giảm không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá trị giảm phải lớn hơn 0")
    private BigDecimal discountValue;

    @DecimalMin(value = "0.0", message = "Giá trị đơn tối thiểu không được âm")
    private BigDecimal minOrderValue;

    private BigDecimal maxDiscountAmount;

    @Min(value = 1, message = "Tổng số lượng voucher phát hành phải từ 1 trở lên")
    private Integer totalLimit;

    @Min(value = 1, message = "Số lượt dùng tối đa / khách phải từ 1 trở lên")
    private Short perUserLimit;

    private ApplicableTo applicableTo;

    private OffsetDateTime validFrom;

    @NotNull(message = "Ngày hết hạn không được để trống")
    private OffsetDateTime validUntil;
}
