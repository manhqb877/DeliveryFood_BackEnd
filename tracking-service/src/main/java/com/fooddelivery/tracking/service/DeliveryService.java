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
                .assignedAt(d.getAssignedAt())
                .pickedUpAt(d.getPickedUpAt())
                .deliveredAt(d.getDeliveredAt())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
