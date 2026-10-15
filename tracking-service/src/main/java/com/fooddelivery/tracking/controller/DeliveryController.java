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

    /**
     * Xác nhận đã lấy hàng tại Quán 1 hoặc Quán 2 (Dành cho đơn ghép / đơn 2 shop).
     */
    @PostMapping("/deliveries/{deliveryId}/confirm-pickup-shop/{shopNumber}")
    public ResponseEntity<DeliveryResponse> confirmPickupShop(
            @PathVariable Long deliveryId,
            @PathVariable int shopNumber,
            @RequestParam Long shipperId) {
        return ResponseEntity.ok(deliveryService.confirmPickupShop(deliveryId, shipperId, shopNumber));
    }

    /**
     * Tối ưu hoá quãng đường ngắn nhất (TSP) cho Shipper ghé 2 quán lấy hàng.
     */
    @PostMapping("/deliveries/{deliveryId}/optimize-route")
    public ResponseEntity<DeliveryResponse> optimizeRoute(
            @PathVariable Long deliveryId,
            @RequestParam(required = false) Double shipperLat,
            @RequestParam(required = false) Double shipperLng) {
        return ResponseEntity.ok(deliveryService.optimizeMultiShopRoute(deliveryId, shipperLat, shipperLng));
    }

    /**
     * Chuyển đổi đơn sang đơn ghép 2 shop (phục vụ kiểm thử và mô phỏng lộ trình đa điểm).
     */
    @PostMapping("/deliveries/{deliveryId}/convert-to-multi-shop")
    public ResponseEntity<DeliveryResponse> convertToMultiShop(
            @PathVariable Long deliveryId,
            @RequestParam(required = false) Long shop2Id,
            @RequestParam(required = false) String shop2Name,
            @RequestParam(required = false) Double pickup2Lat,
            @RequestParam(required = false) Double pickup2Lng,
            @RequestParam(required = false) String pickup2Address) {
        return ResponseEntity.ok(deliveryService.convertToMultiShop(deliveryId, shop2Id, shop2Name, pickup2Lat, pickup2Lng, pickup2Address));
    }

    /**
     * Reset đơn giao hàng về trạng thái ban đầu (ASSIGNED) để phục vụ test/demo lại.
     */
    @PostMapping("/deliveries/{deliveryId}/reset")
    public ResponseEntity<DeliveryResponse> resetDelivery(
            @PathVariable Long deliveryId,
            @RequestParam(required = false) Long shipperId) {
        return ResponseEntity.ok(deliveryService.resetDelivery(deliveryId, shipperId));
    }
}
