package com.fooddelivery.payment.service.impl;

import com.fooddelivery.payment.dto.request.PaymentQrRequest;
import com.fooddelivery.payment.dto.request.SepayWebhookRequest;
import com.fooddelivery.payment.dto.response.PaymentQrResponse;
import com.fooddelivery.payment.entity.Transaction;
import com.fooddelivery.payment.enums.PaymentGateway;
import com.fooddelivery.payment.enums.TransactionStatus;
import com.fooddelivery.payment.enums.TransactionType;
import com.fooddelivery.payment.repository.TransactionRepository;
import com.fooddelivery.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

<<<<<<< HEAD
import com.fooddelivery.payment.dto.event.PaymentEvent;
import com.fooddelivery.payment.kafka.PaymentEventProducer;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
=======
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
>>>>>>> 7e944e4bf810c4b4325503bb97985f09152ab8c8
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final TransactionRepository transactionRepository;
    private final RestTemplate restTemplate;
<<<<<<< HEAD
    private final PaymentEventProducer paymentEventProducer;
=======
>>>>>>> 7e944e4bf810c4b4325503bb97985f09152ab8c8

    @Value("${payment.sepay.bank-name:MBBank}")
    private String bankName;

    @Value("${payment.sepay.account-number:025452790502}")
    private String accountNumber;

    @Value("${payment.sepay.account-holder:NGUYEN THAI AN}")
    private String accountHolder;

    @Value("${payment.sepay.api-token:}")
    private String sepayApiToken;

    @Value("${order.service.url:http://localhost:8083}")
    private String orderServiceUrl;

    // Cache orderCode theo orderId
    private final Map<Long, String> orderCodeCache = new ConcurrentHashMap<>();
    // Rate limit polling SePay cho cùng 1 order (tối thiểu 3s giữa các lần gọi SePay)
    private final Map<Long, Long> lastCheckTimeMap = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public PaymentQrResponse generateSepayQr(PaymentQrRequest request) {
        log.info("Generating SePay VietQR for orderId: {}, orderCode: {}, amount: {}",
                request.getOrderId(), request.getOrderCode(), request.getAmount());

        if (request.getOrderCode() != null) {
            orderCodeCache.put(request.getOrderId(), request.getOrderCode());
        }

        // 1. Kiểm tra hoặc tạo transaction trạng thái PENDING
        Optional<Transaction> existingTx = transactionRepository.findByOrderId(request.getOrderId());
        Transaction transaction;
        if (existingTx.isPresent()) {
            transaction = existingTx.get();
            transaction.setAmount(request.getAmount());
            transaction.setPaymentGateway(PaymentGateway.SEPAY);
            transaction.setTransactionType(TransactionType.ORDER_PAYMENT);
            transaction.setUpdatedAt(OffsetDateTime.now());
        } else {
            transaction = Transaction.builder()
                    .orderId(request.getOrderId())
                    .userId(request.getUserId())
                    .transactionType(TransactionType.ORDER_PAYMENT)
                    .paymentGateway(PaymentGateway.SEPAY)
                    .amount(request.getAmount())
                    .currency("VND")
                    .status(TransactionStatus.PENDING)
                    .idempotencyKey("pay_" + request.getOrderId() + "_" + System.currentTimeMillis())
                    .build();
        }
        transactionRepository.save(transaction);

        // 2. Tạo link VietQR SePay
        long amountLong = request.getAmount().longValue();
        String description = request.getOrderCode();
        String encodedDesc = URLEncoder.encode(description, StandardCharsets.UTF_8);

        // Chuỗi link chuẩn SePay VietQR
        String qrUrl = String.format("https://qr.sepay.vn/img?acc=%s&bank=%s&amount=%d&des=%s",
                accountNumber, bankName, amountLong, encodedDesc);

        return PaymentQrResponse.builder()
                .orderId(request.getOrderId())
                .orderCode(request.getOrderCode())
                .amount(request.getAmount())
                .bankName(bankName)
                .accountNumber(accountNumber)
                .accountHolder(accountHolder)
                .paymentDescription(description)
                .qrUrl(qrUrl)
                .paymentStatus(transaction.getStatus().name())
                .build();
    }

    @Override
    @Transactional
    public boolean handleSepayWebhook(SepayWebhookRequest webhookRequest) {
        log.info("Received SePay Webhook: id={}, amount={}, content={}",
                webhookRequest.getId(), webhookRequest.getTransferAmount(), webhookRequest.getContent());

        if (webhookRequest.getTransferType() != null && !"in".equalsIgnoreCase(webhookRequest.getTransferType())) {
            log.info("Ignoring non-incoming transaction (type={})", webhookRequest.getTransferType());
            return false;
        }

        String rawContent = "";
        if (webhookRequest.getContent() != null) rawContent += " " + webhookRequest.getContent();
        if (webhookRequest.getDescription() != null) rawContent += " " + webhookRequest.getDescription();

        // Trích xuất mã đơn hàng từ nội dung chuyển khoản (Ví dụ: ORD-1727500000-ABCD hoặc ORD1727500000ABCD)
        String orderCode = extractOrderCode(rawContent);
        if (orderCode == null) {
            log.warn("Could not extract order code from transfer content: '{}'", rawContent);
            return false;
        }

        log.info("Extracted orderCode '{}' from SePay transfer content", orderCode);

        // 1. Gọi order-service để tìm đơn hàng
        Map<String, Object> orderData = null;
        try {
            String url = orderServiceUrl + "/orders/code/" + orderCode;
            orderData = restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            log.error("Failed to query order by code {} from order-service: {}", orderCode, e.getMessage());
            return false;
        }

        if (orderData == null || orderData.get("id") == null) {
            log.warn("Order not found in order-service for code: {}", orderCode);
            return false;
        }

        Long orderId = Long.valueOf(orderData.get("id").toString());
        BigDecimal totalAmount = orderData.get("totalAmount") != null 
                ? new BigDecimal(orderData.get("totalAmount").toString()) 
                : BigDecimal.ZERO;

        // 2. Cập nhật hoặc lưu Transaction
        String gatewayTxId = webhookRequest.getId() != null ? String.valueOf(webhookRequest.getId()) : webhookRequest.getReferenceCode();
        Transaction transaction = transactionRepository.findByOrderId(orderId)
                .orElse(Transaction.builder()
                        .orderId(orderId)
                        .transactionType(TransactionType.ORDER_PAYMENT)
                        .paymentGateway(PaymentGateway.SEPAY)
                        .currency("VND")
                        .amount(webhookRequest.getTransferAmount() != null ? webhookRequest.getTransferAmount() : totalAmount)
                        .build());

        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setGatewayTransactionId(gatewayTxId);
        
        Map<String, Object> rawMap = new HashMap<>();
        rawMap.put("id", webhookRequest.getId());
        rawMap.put("transferAmount", webhookRequest.getTransferAmount());
        rawMap.put("content", webhookRequest.getContent());
        rawMap.put("transactionDate", webhookRequest.getTransactionDate());
        rawMap.put("referenceCode", webhookRequest.getReferenceCode());
        transaction.setGatewayResponse(rawMap);
        transaction.setUpdatedAt(OffsetDateTime.now());
        transactionRepository.save(transaction);

        // 3. Thông báo order-service cập nhật trạng thái đơn sang PAID
        try {
            String payUrl = orderServiceUrl + "/orders/" + orderId + "/pay?transactionId=" + gatewayTxId + "&gateway=SEPAY";
            restTemplate.put(payUrl, null);
            log.info("Order {} successfully marked as PAID via SePay", orderId);
        } catch (Exception e) {
<<<<<<< HEAD
            log.error("Failed to update order {} to PAID in order-service via REST: {}. Event will still be published to Kafka.", orderId, e.getMessage());
        }

        // 4. Bắn sự kiện PAYMENT_COMPLETED lên Kafka (chống mất message nếu order-service sập)
        PaymentEvent paymentEvent = PaymentEvent.builder()
                .eventId("pay_evt_" + orderId + "_" + System.currentTimeMillis())
                .eventType("PAYMENT_COMPLETED")
                .orderId(orderId)
                .orderCode(orderCode)
                .userId(transaction.getUserId())
                .gatewayTransactionId(gatewayTxId)
                .paymentGateway("SEPAY")
                .amount(transaction.getAmount())
                .status("SUCCESS")
                .timestamp(Instant.now())
                .build();
        paymentEventProducer.publishPaymentCompleted(paymentEvent);

=======
            log.error("Failed to update order {} to PAID in order-service: {}", orderId, e.getMessage());
        }

>>>>>>> 7e944e4bf810c4b4325503bb97985f09152ab8c8
        return true;
    }

    @Override
    public Transaction getPaymentStatusByOrderId(Long orderId) {
        Optional<Transaction> txOpt = transactionRepository.findByOrderId(orderId);
        if (txOpt.isEmpty()) {
            return null;
        }

        Transaction transaction = txOpt.get();

        // Nếu trạng thái đang PENDING và đã cấu hình SePay API Token -> Poll SePay
        if (transaction.getStatus() == TransactionStatus.PENDING 
                && sepayApiToken != null 
                && !sepayApiToken.trim().isEmpty()
                && !sepayApiToken.contains("YOUR_")) {

            long now = System.currentTimeMillis();
            Long lastCheck = lastCheckTimeMap.get(orderId);
            // Giới hạn tần suất gọi SePay tối thiểu 3 giây/lần cho mỗi đơn
            if (lastCheck == null || (now - lastCheck) >= 3000) {
                lastCheckTimeMap.put(orderId, now);
                checkAndUpdatePaymentFromSepay(transaction);
            }
        }

        return transaction;
    }

    private void checkAndUpdatePaymentFromSepay(Transaction transaction) {
        try {
            String orderCode = getOrderCode(transaction.getOrderId());
            if (orderCode == null || orderCode.trim().isEmpty()) {
                log.warn("Cannot check SePay: orderCode not found for orderId {}", transaction.getOrderId());
                return;
            }

            String sepayUrl = "https://my.sepay.vn/userapi/transactions/list?account_number=" + accountNumber + "&limit=20";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + sepayApiToken.trim());
            headers.set("Content-Type", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(sepayUrl, HttpMethod.GET, entity, Map.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("SePay API returned non-200 or empty response");
                return;
            }

            List<Map<String, Object>> transactions = (List<Map<String, Object>>) response.getBody().get("transactions");
            if (transactions == null || transactions.isEmpty()) {
                return;
            }

            // Chuẩn hóa orderCode (bỏ ký tự đặc biệt, viết hoa)
            String normalizedOrderCode = orderCode.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();

            for (Map<String, Object> txMap : transactions) {
                String content = txMap.get("transaction_content") != null ? txMap.get("transaction_content").toString() : "";
                String normalizedContent = content.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();

                // Kiểm tra nội dung chuyển khoản có chứa mã đơn hàng hoặc orderId
                boolean isMatched = normalizedContent.contains(normalizedOrderCode) 
                        || content.toUpperCase().contains(orderCode.toUpperCase())
                        || normalizedContent.contains("ORD" + transaction.getOrderId())
                        || normalizedContent.contains("DH" + transaction.getOrderId());

                if (isMatched) {
                    BigDecimal amountIn = BigDecimal.ZERO;
                    if (txMap.get("amount_in") != null) {
                        try {
                            amountIn = new BigDecimal(txMap.get("amount_in").toString());
                        } catch (Exception ignored) {}
                    }

                    BigDecimal expectedAmount = transaction.getAmount() != null ? transaction.getAmount() : BigDecimal.ZERO;
                    // Kiểm tra số tiền nhận được >= số tiền đơn hàng
                    if (expectedAmount.compareTo(BigDecimal.ZERO) == 0 || amountIn.compareTo(expectedAmount) >= 0) {
                        log.info("MATCHED SePay transaction for order {}: ID={}, amount={}",
                                transaction.getOrderId(), txMap.get("id"), amountIn);

                        String gatewayTxId = txMap.get("id") != null 
                                ? txMap.get("id").toString() 
                                : String.valueOf(txMap.get("reference_number"));

                        transaction.setStatus(TransactionStatus.SUCCESS);
                        transaction.setGatewayTransactionId(gatewayTxId);
                        transaction.setGatewayResponse(txMap);
                        transaction.setUpdatedAt(OffsetDateTime.now());
                        transactionRepository.save(transaction);

                        // Cập nhật trạng thái đơn hàng sang PAID trong order-service
                        try {
                            String payUrl = orderServiceUrl + "/orders/" + transaction.getOrderId() + "/pay?transactionId=" + gatewayTxId + "&gateway=SEPAY";
                            restTemplate.put(payUrl, null);
                            log.info("Successfully updated order {} to PAID via SePay Polling", transaction.getOrderId());
                        } catch (Exception e) {
<<<<<<< HEAD
                            log.error("Failed to notify order-service for order {}: {}. Event will still be published to Kafka.", transaction.getOrderId(), e.getMessage());
                        }

                        // Bắn sự kiện PAYMENT_COMPLETED lên Kafka (chống mất message nếu order-service sập)
                        PaymentEvent paymentEvent = PaymentEvent.builder()
                                .eventId("pay_evt_" + transaction.getOrderId() + "_" + System.currentTimeMillis())
                                .eventType("PAYMENT_COMPLETED")
                                .orderId(transaction.getOrderId())
                                .orderCode(orderCode)
                                .userId(transaction.getUserId())
                                .gatewayTransactionId(gatewayTxId)
                                .paymentGateway("SEPAY")
                                .amount(transaction.getAmount())
                                .status("SUCCESS")
                                .timestamp(Instant.now())
                                .build();
                        paymentEventProducer.publishPaymentCompleted(paymentEvent);

=======
                            log.error("Failed to notify order-service for order {}: {}", transaction.getOrderId(), e.getMessage());
                        }

>>>>>>> 7e944e4bf810c4b4325503bb97985f09152ab8c8
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error polling SePay transactions for order {}: {}", transaction.getOrderId(), e.getMessage());
        }
    }

    private String getOrderCode(Long orderId) {
        if (orderCodeCache.containsKey(orderId)) {
            return orderCodeCache.get(orderId);
        }
        try {
            String url = orderServiceUrl + "/orders/" + orderId;
            Map<String, Object> orderData = restTemplate.getForObject(url, Map.class);
            if (orderData != null && orderData.get("orderCode") != null) {
                String code = orderData.get("orderCode").toString();
                orderCodeCache.put(orderId, code);
                return code;
            }
        } catch (Exception e) {
            log.warn("Could not fetch orderCode from order-service for orderId {}: {}", orderId, e.getMessage());
        }
        return null;
    }

    private String extractOrderCode(String text) {
        if (text == null) return null;
        Pattern pattern = Pattern.compile("(?i)(ORD[-_]?[0-9]{5,15}[-_]?[0-9A-Z]{2,8})");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase();
        }

        Pattern patternShort = Pattern.compile("(?i)(ORD[-_]?[0-9A-Z_-]+)");
        Matcher matcherShort = patternShort.matcher(text);
        if (matcherShort.find()) {
            return matcherShort.group(1).toUpperCase();
        }
        return null;
    }
}
