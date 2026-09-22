package com.fooddelivery.tracking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationUpdateResponse {

    private Long shipperId;
    private Double lat;
    private Double lng;
    private Long deliveryId;
    private Instant updatedAt;
    private String message;
}
