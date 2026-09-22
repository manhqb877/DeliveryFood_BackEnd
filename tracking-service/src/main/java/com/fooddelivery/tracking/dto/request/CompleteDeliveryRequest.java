package com.fooddelivery.tracking.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompleteDeliveryRequest {

    /** URL hoặc base64 của ảnh xác nhận giao hàng */
    private String proofPhotoUrl;

    /** OTP xác nhận từ khách (nếu dùng OTP mode) */
    private String otp;

    /** Ghi chú thêm (nếu có) */
    private String note;
}
