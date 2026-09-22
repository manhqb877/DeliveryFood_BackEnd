package com.fooddelivery.tracking.controller;

import com.fooddelivery.tracking.dto.request.UpdateLocationRequest;
import com.fooddelivery.tracking.dto.response.LocationUpdateResponse;
import com.fooddelivery.tracking.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API cập nhật và truy vấn vị trí GPS của Shipper.
 * Được AppShipper gọi định kỳ mỗi 5-10 giây.
 */
@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/tracking/shippers")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    /**
     * Shipper gửi tọa độ GPS hiện tại lên server.
     * AppShipper gọi API này mỗi 5-10 giây.
     *
     * @param shipperId ID của shipper
     * @param req       Thông tin GPS
     */
    @PatchMapping("/{shipperId}/location")
    public ResponseEntity<LocationUpdateResponse> updateLocation(
            @PathVariable Long shipperId,
            @Valid @RequestBody UpdateLocationRequest req) {
        log.debug("[LocationController] Received GPS update from shipper {}: lat={}, lng={}", shipperId, req.getLat(), req.getLng());
        return ResponseEntity.ok(locationService.updateLocation(shipperId, req));
    }

    /**
     * Lấy vị trí hiện tại của Shipper từ Redis (dành cho Web Customer / Shop tracking).
     * Trả về raw JSON string hoặc 404 nếu shipper offline.
     *
     * @param shipperId ID của shipper
     */
    @GetMapping("/{shipperId}/location")
    public ResponseEntity<String> getShipperLocation(@PathVariable Long shipperId) {
        String location = locationService.getLocationFromRedis(shipperId);
        if (location == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .body(location);
    }
}
