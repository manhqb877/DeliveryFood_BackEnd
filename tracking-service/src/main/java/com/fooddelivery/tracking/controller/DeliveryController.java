package com.fooddelivery.tracking.controller;

import com.fooddelivery.tracking.dto.request.CompleteDeliveryRequest;
import com.fooddelivery.tracking.dto.response.DeliveryResponse;
import com.fooddelivery.tracking.dto.response.WalletResponse;
import com.fooddelivery.tracking.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API quản lý đơn giao hàng cho AppShipper.
 * Cung cấp các endpoint để Shipper xem danh sách, nhận và hoàn thành đơn.
 */
@Slf4j
@RestController
@RequestMapping("/tracking")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    /**
     * Lấy danh sách đơn hàng đang chờ Shipper nhận.
     */
    @GetMapping("/deliveries/available")
    public ResponseEntity<List<DeliveryResponse>> getAvailableDeliveries() {
        return ResponseEntity.ok(deliveryService.getAvailableDeliveries());
    }

    /**
     * Lấy chi tiết một đơn hàng.
     */
    @GetMapping("/deliveries/{deliveryId}")
    public ResponseEntity<DeliveryResponse> getDelivery(@PathVariable Long deliveryId) {
        return ResponseEntity.ok(deliveryService.getDeliveryById(deliveryId));
    }

    /**
     * Lấy thông tin delivery theo orderId (Dành cho Customer/Shop tracking).
     */
    @GetMapping("/deliveries/order/{orderId}")
    public ResponseEntity<DeliveryResponse> getDeliveryByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(deliveryService.getDeliveryByOrderId(orderId));
    }

    /**
     * Lấy danh sách đơn đang hoạt động của Shipper.
     */
    @GetMapping("/shippers/{shipperId}/deliveries/active")
    public ResponseEntity<List<DeliveryResponse>> getActiveDeliveries(@PathVariable Long shipperId) {
        return ResponseEntity.ok(deliveryService.getActiveDeliveriesByShipper(shipperId));
    }

    /**
     * Lấy danh sách lịch sử đơn hàng của Shipper.
     */
    @GetMapping("/shippers/{shipperId}/history")
    public ResponseEntity<List<DeliveryResponse>> getHistoryDeliveries(@PathVariable Long shipperId) {
        return ResponseEntity.ok(deliveryService.getHistoryDeliveries(shipperId));
    }

    /**
     * Lấy thông tin ví COD của Shipper.
     */
    @GetMapping("/shippers/{shipperId}/wallet")
    public ResponseEntity<WalletResponse> getWalletInfo(@PathVariable Long shipperId) {
        return ResponseEntity.ok(deliveryService.getWalletInfo(shipperId));
    }

    /**
     * Shipper nhận đơn.
     * Body: { "shipperId": 123 }
     */
    @PostMapping("/deliveries/{deliveryId}/accept")
    public ResponseEntity<DeliveryResponse> acceptDelivery(
            @PathVariable Long deliveryId,
            @RequestParam Long shipperId) {
        return ResponseEntity.ok(deliveryService.acceptDelivery(deliveryId, shipperId));
    }

    /**
     * Shipper bắt đầu đi lấy hàng.
     */
    @PostMapping("/deliveries/{deliveryId}/start-pickup")
    public ResponseEntity<DeliveryResponse> startPickup(
            @PathVariable Long deliveryId,
            @RequestParam Long shipperId) {
        return ResponseEntity.ok(deliveryService.startPickup(deliveryId, shipperId));
    }

    /**
     * Shipper xác nhận đã lấy hàng xong.
     */
    @PostMapping("/deliveries/{deliveryId}/confirm-pickup")
    public ResponseEntity<DeliveryResponse> confirmPickup(
            @PathVariable Long deliveryId,
            @RequestParam Long shipperId) {
        return ResponseEntity.ok(deliveryService.confirmPickup(deliveryId, shipperId));
    }

    /**
     * Shipper hoàn thành giao hàng, upload ảnh xác nhận.
     */
    @PostMapping("/deliveries/{deliveryId}/complete")
    public ResponseEntity<DeliveryResponse> completeDelivery(
            @PathVariable Long deliveryId,
            @RequestParam Long shipperId,
            @RequestBody(required = false) CompleteDeliveryRequest req) {
        if (req == null) req = new CompleteDeliveryRequest();
        return ResponseEntity.ok(deliveryService.completeDelivery(deliveryId, shipperId, req));
    }
}
