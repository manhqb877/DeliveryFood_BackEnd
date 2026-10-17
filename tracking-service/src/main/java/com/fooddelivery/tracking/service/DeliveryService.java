package com.fooddelivery.tracking.service;

import com.fooddelivery.tracking.dto.request.CompleteDeliveryRequest;
import com.fooddelivery.tracking.dto.response.DeliveryResponse;
import com.fooddelivery.tracking.dto.response.WalletResponse;
import com.fooddelivery.tracking.entity.Delivery;
import com.fooddelivery.tracking.enums.DeliveryStatus;
import com.fooddelivery.tracking.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service quản lý đơn giao hàng (Delivery).
 * Cung cấp các API cho AppShipper:
 *   - Lấy danh sách đơn chờ
 *   - Nhận đơn
 *   - Cập nhật trạng thái (đã lấy hàng, đang giao, hoàn thành)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final StringRedisTemplate redisTemplate;

    /**
     * Lấy tất cả đơn hàng đang chờ shipper nhận (PENDING, chưa có shipper).
     */
    public List<DeliveryResponse> getAvailableDeliveries() {
        return deliveryRepository.findAvailableDeliveries().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách đơn hàng đang hoạt động của một Shipper cụ thể.
     */
    public List<DeliveryResponse> getActiveDeliveriesByShipper(Long shipperId) {
        return deliveryRepository.findActiveDeliveriesByShipper(shipperId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách đơn hàng đã hoàn thành, huỷ hoặc thất bại của Shipper.
     */
    public List<DeliveryResponse> getHistoryDeliveries(Long shipperId) {
        return deliveryRepository.findHistoryDeliveriesByShipper(shipperId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy thông tin ví tiền COD của Shipper (tổng COD của các đơn DELIVERED).
     */
    public WalletResponse getWalletInfo(Long shipperId) {
        List<Delivery> delivered = deliveryRepository.findHistoryDeliveriesByShipper(shipperId).stream()
                .filter(d -> d.getStatus() == DeliveryStatus.DELIVERED)
                .collect(Collectors.toList());
        
        java.math.BigDecimal totalCod = java.math.BigDecimal.ZERO;
        for (Delivery d : delivered) {
            if (d.getCodAmount() != null) {
                totalCod = totalCod.add(d.getCodAmount());
            }
        }
        
        return WalletResponse.builder()
                .shipperId(shipperId)
                .totalCodCollected(totalCod)
                .totalCompletedDeliveries(delivered.size())
                .build();
    }

    /**
     * Lấy chi tiết một đơn hàng.
     */
    public DeliveryResponse getDeliveryById(Long deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + deliveryId));
        return toResponse(delivery);
    }

    /**
     * Lấy thông tin tracking theo orderId.
     */
    public DeliveryResponse getDeliveryByOrderId(Long orderId) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Chưa có thông tin giao hàng cho đơn: " + orderId));
        return toResponse(delivery);
    }

    /**
     * Shipper nhận đơn hàng.
     * Chuyển trạng thái: PENDING -> ASSIGNED
     */
    @Transactional
    public DeliveryResponse acceptDelivery(Long deliveryId, Long shipperId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + deliveryId));

        if (delivery.getStatus() != DeliveryStatus.PENDING) {
            throw new IllegalStateException("Đơn hàng này đã được nhận bởi shipper khác");
        }

        if (!Boolean.TRUE.equals(delivery.getReadyForShipper())) {
            throw new IllegalStateException("Đơn hàng chưa được các quán xác nhận chuẩn bị món!");
        }

        delivery.setShipperId(shipperId);
        delivery.setStatus(DeliveryStatus.ASSIGNED);
        delivery.setAssignedAt(OffsetDateTime.now());

        Delivery saved = deliveryRepository.save(delivery);
        log.info("[DeliveryService] Shipper {} accepted delivery {}", shipperId, deliveryId);

        return toResponse(saved);
    }

    /**
     * Shipper bắt đầu đi lấy hàng.
     * Chuyển trạng thái: ASSIGNED -> GOING_PICKUP
     */
    @Transactional
    public DeliveryResponse startPickup(Long deliveryId, Long shipperId) {
        Delivery delivery = getAndValidate(deliveryId, shipperId);
        delivery.setStatus(DeliveryStatus.GOING_PICKUP);
        delivery.setGoingPickupAt(OffsetDateTime.now());
        return toResponse(deliveryRepository.save(delivery));
    }

    /**
     * Shipper đã lấy hàng.
     * Chuyển trạng thái: GOING_PICKUP | AT_SHOP -> PICKED_UP
     */
    @Transactional
    public DeliveryResponse confirmPickup(Long deliveryId, Long shipperId) {
        Delivery delivery = getAndValidate(deliveryId, shipperId);
        delivery.setStatus(DeliveryStatus.PICKED_UP);
        delivery.setPickedUpAt(OffsetDateTime.now());
        Delivery saved = deliveryRepository.save(delivery);
        
        // Gọi sang order-service để báo "DELIVERING"
        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            java.util.Map<String, String> body = new java.util.HashMap<>();
            body.put("status", "DELIVERING");
            body.put("actorType", "SHIPPER");
            restTemplate.put("http://order-service:8083/orders/" + delivery.getOrderId() + "/status", body);
        } catch (Exception e) {
            log.error("Lỗi khi update status order sang DELIVERING: {}", e.getMessage());
        }
        
        return toResponse(saved);
    }

    /**
     * Shipper hoàn thành giao hàng (kèm ảnh xác nhận).
     * Chuyển trạng thái: PICKED_UP | DELIVERING -> DELIVERED
     */
    @Transactional
    public DeliveryResponse completeDelivery(Long deliveryId, Long shipperId, CompleteDeliveryRequest req) {
        Delivery delivery = getAndValidate(deliveryId, shipperId);
        delivery.setStatus(DeliveryStatus.DELIVERED);
        delivery.setDeliveredAt(OffsetDateTime.now());
        if (req.getProofPhotoUrl() != null) {
            delivery.setProofPhotoUrl(req.getProofPhotoUrl());
        }
        Delivery saved = deliveryRepository.save(delivery);
        log.info("[DeliveryService] Delivery {} completed by shipper {}", deliveryId, shipperId);

        // Xoá live location trong Redis sau khi giao xong
        redisTemplate.delete("shipper:location:" + shipperId);

        // Gọi sang order-service để báo "DELIVERED"
        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            java.util.Map<String, String> body = new java.util.HashMap<>();
            body.put("status", "DELIVERED");
            body.put("actorType", "SHIPPER");
            restTemplate.put("http://order-service:8083/orders/" + delivery.getOrderId() + "/status", body);
        } catch (Exception e) {
            log.error("Lỗi khi update status order sang DELIVERED: {}", e.getMessage());
        }

        return toResponse(saved);
    }

    // ──────────── Private helpers ──────────────

        /**
     * Reset đơn giao hàng về trạng thái ASSIGNED để test demo lại từ đầu.
     */
    @Transactional
    public DeliveryResponse resetDelivery(Long deliveryId, Long shipperId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + deliveryId));
        delivery.setStatus(DeliveryStatus.ASSIGNED);
        delivery.setGoingPickupAt(null);
        delivery.setPickedUpAt(null);
        delivery.setDeliveredAt(null);
        delivery.setProofPhotoUrl(null);
        delivery.setPickup1Completed(false);
        delivery.setPickup2Completed(false);
        Delivery saved = deliveryRepository.save(delivery);
        log.info("[DeliveryService] Reset delivery {} to ASSIGNED for demo testing", deliveryId);

        // Gọi sang order-service để reset trạng thái đơn sang ASSIGNED hoặc READY_FOR_PICKUP
        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            java.util.Map<String, String> body = new java.util.HashMap<>();
            body.put("status", "READY_FOR_PICKUP");
            body.put("actorType", "SHOP");
            restTemplate.put("http://order-service:8083/orders/" + delivery.getOrderId() + "/status", body);
        } catch (Exception e) {
            log.error("Lỗi khi reset status order: {}", e.getMessage());
        }

        return toResponse(saved);
    }

    /**
     * Xác nhận đã lấy hàng tại Shop 1 hoặc Shop 2 trong đơn ghép / đa điểm.
     */
    @Transactional
    public DeliveryResponse confirmPickupShop(Long deliveryId, Long shipperId, int shopNumber) {
        Delivery delivery = getAndValidate(deliveryId, shipperId);
        if (shopNumber == 1) {
            delivery.setPickup1Completed(true);
        } else if (shopNumber == 2) {
            delivery.setPickup2Completed(true);
        }

        boolean isMulti = Boolean.TRUE.equals(delivery.getIsMultiShop());
        boolean p1Done = Boolean.TRUE.equals(delivery.getPickup1Completed());
        boolean p2Done = Boolean.TRUE.equals(delivery.getPickup2Completed());

        // Nếu đã hoàn tất lấy hàng cả 2 shop (hoặc đơn thường chỉ có 1 shop)
        if ((!isMulti && p1Done) || (isMulti && p1Done && p2Done)) {
            delivery.setStatus(DeliveryStatus.PICKED_UP);
            delivery.setPickedUpAt(OffsetDateTime.now());

            try {
                org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
                java.util.Map<String, String> body = new java.util.HashMap<>();
                body.put("status", "DELIVERING");
                body.put("actorType", "SHIPPER");
                restTemplate.put("http://order-service:8083/orders/" + delivery.getOrderId() + "/status", body);
            } catch (Exception e) {
                log.error("Lỗi khi update status order sang DELIVERING: {}", e.getMessage());
            }
        } else {
            // Vẫn còn shop tiếp theo cần ghé lấy
            delivery.setStatus(DeliveryStatus.GOING_PICKUP);
        }

        Delivery saved = deliveryRepository.save(delivery);
        log.info("[DeliveryService] Shipper {} confirmed pickup at shop {} for delivery {}. Status: {}",
                shipperId, shopNumber, deliveryId, saved.getStatus());
        return toResponse(saved);
    }

    /**
     * Thuật toán tối ưu hoá lộ trình ngắn nhất (TSP) cho Shipper lấy hàng từ 2 quán trước khi giao cho khách:
     * Tuyến 1: Shipper -> Shop 1 -> Shop 2 -> Khách
     * Tuyến 2: Shipper -> Shop 2 -> Shop 1 -> Khách
     * Lựa chọn tuyến có tổng khoảng cách nhỏ nhất.
     */
    @Transactional
    public DeliveryResponse optimizeMultiShopRoute(Long deliveryId, Double shipperLat, Double shipperLng) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + deliveryId));

        if (!Boolean.TRUE.equals(delivery.getIsMultiShop()) || delivery.getPickup2Lat() == null) {
            return toResponse(delivery);
        }

        double sLat = shipperLat != null ? shipperLat : (delivery.getPickupLat() != null ? delivery.getPickupLat() : 10.7733);
        double sLng = shipperLng != null ? shipperLng : (delivery.getPickupLng() != null ? delivery.getPickupLng() : 106.6977);
        double p1Lat = delivery.getPickupLat();
        double p1Lng = delivery.getPickupLng();
        double p2Lat = delivery.getPickup2Lat();
        double p2Lng = delivery.getPickup2Lng();
        double dLat = delivery.getDeliveryLat();
        double dLng = delivery.getDeliveryLng();

        // Tuyến 1: S -> P1 -> P2 -> D
        double dist1 = calculateDistanceKm(sLat, sLng, p1Lat, p1Lng)
                     + calculateDistanceKm(p1Lat, p1Lng, p2Lat, p2Lng)
                     + calculateDistanceKm(p2Lat, p2Lng, dLat, dLng);

        // Tuyến 2: S -> P2 -> P1 -> D
        double dist2 = calculateDistanceKm(sLat, sLng, p2Lat, p2Lng)
                     + calculateDistanceKm(p2Lat, p2Lng, p1Lat, p1Lng)
                     + calculateDistanceKm(p1Lat, p1Lng, dLat, dLng);

        String optimalOrder = (dist1 <= dist2) ? "SHOP1_FIRST" : "SHOP2_FIRST";
        delivery.setOptimalPickupOrder(optimalOrder);
        delivery.setEstimatedDistanceM((int) Math.round(Math.min(dist1, dist2) * 1000));
        Delivery saved = deliveryRepository.save(delivery);

        log.info("[DeliveryService] Optimal route calculated for delivery {}: order={}, option1={}km, option2={}km (best={}km)",
                deliveryId, optimalOrder, String.format("%.2f", dist1), String.format("%.2f", dist2),
                String.format("%.2f", Math.min(dist1, dist2)));

        return toResponse(saved);
    }

    /**
     * Chuyển đổi đơn thường thành đơn ghép 2 shop để test hoặc xử lý đơn liên quán.
     */
    @Transactional
    public DeliveryResponse convertToMultiShop(Long deliveryId, Long shop2Id, String shop2Name,
                                              Double pickup2Lat, Double pickup2Lng, String pickup2Address) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + deliveryId));

        delivery.setIsMultiShop(true);
        delivery.setShop1Id(delivery.getShop1Id() != null ? delivery.getShop1Id() : 39L);
        delivery.setShop1Name(delivery.getShop1Name() != null ? delivery.getShop1Name() : delivery.getPickupAddress().split(" - ")[0]);
        delivery.setPickup1Completed(false);

        delivery.setShop2Id(shop2Id != null ? shop2Id : 42L);
        delivery.setShop2Name(shop2Name != null ? shop2Name : "TAKA CHA - Trà Sữa & Chè Sầu Riêng");
        delivery.setPickup2Lat(pickup2Lat != null ? pickup2Lat : 10.77173);
        delivery.setPickup2Lng(pickup2Lng != null ? pickup2Lng : 106.70036);
        delivery.setPickup2Address(pickup2Address != null ? pickup2Address : "66/1 Huỳnh Thúc Kháng, P.Bến Thành, Q.1, Hồ Chí Minh");
        delivery.setPickup2Completed(false);

        // Tự động tính toán lộ trình tối ưu ban đầu
        optimizeMultiShopRoute(delivery.getId(), delivery.getPickupLat(), delivery.getPickupLng());

        Delivery saved = deliveryRepository.findById(deliveryId).orElse(delivery);
        log.info("[DeliveryService] Converted delivery {} to Multi-Shop (2 shops)", deliveryId);
        return toResponse(saved);
    }

    private double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private Delivery getAndValidate(Long deliveryId, Long shipperId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng: " + deliveryId));
        if (!shipperId.equals(delivery.getShipperId())) {
            throw new IllegalStateException("Bạn không có quyền cập nhật đơn hàng này");
        }
        return delivery;
    }

    private DeliveryResponse toResponse(Delivery d) {
        return DeliveryResponse.builder()
                .id(d.getId())
                .orderId(d.getOrderId())
                .orderCode(d.getOrderCode())
                .shipperId(d.getShipperId())
                .areaId(d.getAreaId())
                .pickupLat(d.getPickupLat())
                .pickupLng(d.getPickupLng())
                .pickupAddress(d.getPickupAddress())
                .shop1Id(d.getShop1Id())
                .shop1Name(d.getShop1Name())
                .pickup1Completed(d.getPickup1Completed())
                .isMultiShop(d.getIsMultiShop())
                .pickup2Lat(d.getPickup2Lat())
                .pickup2Lng(d.getPickup2Lng())
                .pickup2Address(d.getPickup2Address())
                .shop2Id(d.getShop2Id())
                .shop2Name(d.getShop2Name())
                .pickup2Completed(d.getPickup2Completed())
                .optimalPickupOrder(d.getOptimalPickupOrder())
                .deliveryLat(d.getDeliveryLat())
                .deliveryLng(d.getDeliveryLng())
                .deliveryAddress(d.getDeliveryAddress())
                .deliveryBuilding(d.getDeliveryBuilding())
                .deliveryFloor(d.getDeliveryFloor())
                .deliveryUnit(d.getDeliveryUnit())
                .deliveryGate(d.getDeliveryGate())
                .estimatedDistanceM(d.getEstimatedDistanceM())
                .estimatedDurationS(d.getEstimatedDurationS())
                .currentEtaMinutes(d.getCurrentEtaMinutes())
                .codAmount(d.getCodAmount())
                .codCollected(d.getCodCollected())
                .status(d.getStatus() != null ? d.getStatus().name() : null)
                .confirmMethod(d.getConfirmMethod() != null ? d.getConfirmMethod().name() : null)
                .deliveryOtp(d.getDeliveryOtp())
                .proofPhotoUrl(d.getProofPhotoUrl())
                .shop1Confirmed(d.getShop1Confirmed())
                .shop2Confirmed(d.getShop2Confirmed())
                .readyForShipper(d.getReadyForShipper())
                .assignedAt(d.getAssignedAt())
                .pickedUpAt(d.getPickedUpAt())
                .deliveredAt(d.getDeliveredAt())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
