package com.fooddelivery.tracking.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateLocationRequest {

    @NotNull
    private Double lat;

    @NotNull
    private Double lng;

    private Float accuracyM;
    private Float headingDeg;
    private Float speedMs;
    private Float altitudeM;

    /** Timestamp GPS từ thiết bị client (millis epoch) */
    private Long deviceTimestamp;

    /** ID đơn hàng đang giao (nếu có) */
    private Long deliveryId;

    /** Trạng thái online: true = đang nhận đơn, false = offline */
    private Boolean isOnline;
}
