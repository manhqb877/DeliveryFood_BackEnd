package com.fooddelivery.tracking.service;

import com.fooddelivery.tracking.document.LocationSnapshotDocument;
import com.fooddelivery.tracking.dto.request.UpdateLocationRequest;
import com.fooddelivery.tracking.dto.response.LocationUpdateResponse;
import com.fooddelivery.tracking.entity.LiveLocation;
import com.fooddelivery.tracking.repository.LiveLocationRepository;
import com.fooddelivery.tracking.repository.LocationSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Dịch vụ xử lý cập nhật vị trí GPS của Shipper.
 * Luồng:
 *   1. Lưu ngay vào Redis (hot path) cho query nhanh
 *   2. Upsert bảng live_locations (Postgres) để persist vị trí hiện tại
 *   3. Ghi snapshot lịch sử vào MongoDB (async)
 *   4. Phát sự kiện qua STOMP WebSocket cho các client đang xem bản đồ
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    private static final String REDIS_LOCATION_KEY = "shipper:location:%d";
    private static final long REDIS_TTL_SECONDS = 300; // 5 phút timeout nếu shipper mất kết nối

    private final LiveLocationRepository liveLocationRepository;
    private final LocationSnapshotRepository locationSnapshotRepository;
    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Cập nhật vị trí real-time của Shipper.
     */
    @Transactional
    public LocationUpdateResponse updateLocation(Long shipperId, UpdateLocationRequest req) {
        Instant now = Instant.now();

        // 1. Lưu vị trí vào Redis
        String redisKey = String.format(REDIS_LOCATION_KEY, shipperId);
        String locationJson = buildLocationJson(shipperId, req, now);
        redisTemplate.opsForValue().set(redisKey, locationJson, REDIS_TTL_SECONDS, TimeUnit.SECONDS);

        // 2. Phát trực tiếp qua STOMP WebSocket (Frontend subscribe vào /topic/shippers/{id})
        messagingTemplate.convertAndSend("/topic/shippers/" + shipperId, locationJson);
        log.debug("[LocationService] Published via STOMP for shipper {}", shipperId);

        // 3. Upsert live_location trong Postgres
        upsertLiveLocation(shipperId, req, now);

        // 4. Ghi snapshot vào MongoDB (bất đồng bộ - không blocking hot path)
        saveSnapshotAsync(shipperId, req, now);

        return LocationUpdateResponse.builder()
                .shipperId(shipperId)
                .lat(req.getLat())
                .lng(req.getLng())
                .deliveryId(req.getDeliveryId())
                .updatedAt(now)
                .message("OK")
                .build();
    }

    /**
     * Lấy vị trí hiện tại của Shipper (ưu tiên từ Redis).
     */
    public String getLocationFromRedis(Long shipperId) {
        String redisKey = String.format(REDIS_LOCATION_KEY, shipperId);
        return redisTemplate.opsForValue().get(redisKey);
    }

    // ──────────── Private helpers ──────────────

    private void upsertLiveLocation(Long shipperId, UpdateLocationRequest req, Instant now) {
        LiveLocation location = liveLocationRepository.findByShipperId(shipperId)
                .orElseGet(() -> LiveLocation.builder().shipperId(shipperId).build());

        location.setLat(req.getLat());
        location.setLng(req.getLng());
        location.setDeliveryId(req.getDeliveryId());
        location.setIsOnline(req.getIsOnline() != null ? req.getIsOnline() : true);
        location.setServerReceivedAt(OffsetDateTime.now());
        location.setDeviceTimestamp(
                req.getDeviceTimestamp() != null
                        ? OffsetDateTime.ofInstant(Instant.ofEpochMilli(req.getDeviceTimestamp()), java.time.ZoneOffset.UTC)
                        : OffsetDateTime.now()
        );

        if (req.getAccuracyM() != null) location.setAccuracyM(req.getAccuracyM());
        if (req.getHeadingDeg() != null) location.setHeadingDeg(req.getHeadingDeg());
        if (req.getSpeedMs() != null) location.setSpeedMs(req.getSpeedMs());
        if (req.getAltitudeM() != null) location.setAltitudeM(req.getAltitudeM());

        liveLocationRepository.save(location);
    }

    @Async
    protected void saveSnapshotAsync(Long shipperId, UpdateLocationRequest req, Instant now) {
        try {
            LocationSnapshotDocument snapshot = LocationSnapshotDocument.builder()
                    .lat(req.getLat())
                    .lng(req.getLng())
                    .serverReceivedAt(now)
                    .deviceTimestamp(req.getDeviceTimestamp() != null
                            ? Instant.ofEpochMilli(req.getDeviceTimestamp()) : now)
                    .accuracyM(req.getAccuracyM() != null ? req.getAccuracyM().doubleValue() : null)
                    .headingDeg(req.getHeadingDeg() != null ? req.getHeadingDeg().doubleValue() : null)
                    .speedMs(req.getSpeedMs() != null ? req.getSpeedMs().doubleValue() : null)
                    .meta(LocationSnapshotDocument.LocationMeta.builder()
                            .shipperId(shipperId)
                            .deliveryId(req.getDeliveryId())
                            .build())
                    .build();
            locationSnapshotRepository.save(snapshot);
        } catch (Exception e) {
            log.warn("[LocationService] Failed to save MongoDB snapshot for shipper {}: {}", shipperId, e.getMessage());
        }
    }

    private String buildLocationJson(Long shipperId, UpdateLocationRequest req, Instant now) {
        return String.format(
                "{\"shipperId\":%d,\"lat\":%.6f,\"lng\":%.6f,\"deliveryId\":%s,\"timestamp\":\"%s\",\"heading\":%s,\"speed\":%s}",
                shipperId,
                req.getLat(),
                req.getLng(),
                req.getDeliveryId() != null ? req.getDeliveryId().toString() : "null",
                now.toString(),
                req.getHeadingDeg() != null ? req.getHeadingDeg().toString() : "null",
                req.getSpeedMs() != null ? req.getSpeedMs().toString() : "null"
        );
    }
}
