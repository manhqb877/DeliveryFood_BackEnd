package com.fooddelivery.tracking.entity;



import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "live_locations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveLocation {

    @Id
    @Column(name = "shipper_id")
    private Long shipperId; // Khóa chính (1 row duy nhất mỗi Shipper)

    @Column(name = "delivery_id")
    private Long deliveryId; // Đơn đang giao (NULL khi offline)

    // Tọa độ GPS realtime
    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    @Column(name = "accuracy_m")
    private Float accuracyM;

    @Column(name = "heading_deg")
    private Float headingDeg;

    @Column(name = "speed_ms")
    private Float speedMs;

    @Column(name = "altitude_m")
    private Float altitudeM;

    @Column(name = "eta_to_pickup_s")
    private Integer etaToPickupS;

    @Column(name = "eta_to_delivery_s")
    private Integer etaToDeliveryS;

    @Column(name = "is_online", nullable = false)
    private Boolean isOnline = true;

    @Column(name = "device_timestamp", nullable = false)
    private OffsetDateTime deviceTimestamp; // Client GPS time

    @Column(name = "server_received_at", nullable = false)
    private OffsetDateTime serverReceivedAt = OffsetDateTime.now(); // Server time
}