package com.fooddelivery.tracking.document;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document(collection = "location_snapshots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationSnapshotDocument {

    @Id
    private org.bson.types.ObjectId id;

    @Field("device_timestamp")
    private Instant deviceTimestamp; // GPS timestamp từ thiết bị

    @Field("server_received_at")
    private Instant serverReceivedAt; // Server nhận GPS

    @Field("meta")
    private LocationMeta meta;

    private Double lat;
    private Double lng;

    @Field("accuracy_m")
    private Double accuracyM;

    @Field("heading_deg")
    private Double headingDeg;

    @Field("speed_ms")
    private Double speedMs;

    @Field("altitude_m")
    private Double altitudeM;

    @Field("delivery_status")
    private String deliveryStatus;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LocationMeta {
        @Field("shipper_id")
        private Long shipperId;

        @Field("delivery_id")
        private Long deliveryId;

        @Field("area_id")
        private Long areaId;
    }
}