package com.fooddelivery.payment.controller;

import com.fooddelivery.payment.dto.request.PaymentQrRequest;
import com.fooddelivery.payment.dto.request.SepayWebhookRequest;
import com.fooddelivery.payment.dto.response.ApiResponse;
import com.fooddelivery.payment.dto.response.PaymentQrResponse;
import com.fooddelivery.payment.entity.Transaction;
import com.fooddelivery.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/sepay/qr")
    public ResponseEntity<ApiResponse> generateSepayQr(@Valid @RequestBody PaymentQrRequest request) {
        PaymentQrResponse response = paymentService.generateSepayQr(request);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Tạo mã QR SePay thành công")
                .data(response)
                .build());
    }

    @PostMapping("/sepay/webhook")
    public ResponseEntity<Map<String, Object>> handleSepayWebhook(@RequestBody SepayWebhookRequest webhookRequest) {
        log.info("Received webhook call from SePay: {}", webhookRequest);
        boolean success = paymentService.handleSepayWebhook(webhookRequest);
        return ResponseEntity.ok(Map.of(
                "success", success,
                "message", success ? "Xử lý webhook thành công" : "Bỏ qua hoặc không tìm thấy đơn tương ứng"
        ));
    }

    @GetMapping("/status/{orderId}")
    public ResponseEntity<ApiResponse> getPaymentStatus(@PathVariable Long orderId) {
        Transaction tx = paymentService.getPaymentStatusByOrderId(orderId);
        if (tx == null) {
            return ResponseEntity.ok(ApiResponse.builder()
                    .status(HttpStatus.OK.value())
                    .message("Chưa có giao dịch")
                    .data(Map.of("orderId", orderId, "status", "NOT_FOUND"))
                    .build());
        }
        return ResponseEntity.ok(ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Lấy trạng thái giao dịch thành công")
                .data(Map.of(
                        "orderId", tx.getOrderId(),
                        "status", tx.getStatus().name(),
                        "amount", tx.getAmount(),
                        "gateway", tx.getPaymentGateway() != null ? tx.getPaymentGateway().name() : "UNKNOWN",
                        "gatewayTransactionId", tx.getGatewayTransactionId() != null ? tx.getGatewayTransactionId() : ""
                ))
                .build());
    }
}
