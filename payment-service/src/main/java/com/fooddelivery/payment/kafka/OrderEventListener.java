package com.fooddelivery.payment.kafka;

import com.fooddelivery.payment.dto.event.OrderEvent;
import com.fooddelivery.payment.entity.Transaction;
import com.fooddelivery.payment.enums.PaymentGateway;
import com.fooddelivery.payment.enums.TransactionStatus;
import com.fooddelivery.payment.enums.TransactionType;
import com.fooddelivery.payment.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final TransactionRepository transactionRepository;

    /**
     * Consume order-events từ Kafka.
     * Sử dụng @RetryableTopic:
     * - Tự động retry 3 lần với exponential backoff (1s, 2s, 4s).
     * - Nếu thanh toán hoặc hệ thống gặp lỗi tạm thời (payment sập), Kafka giữ message, retry khi phục hồi.
     * - Nếu thất bại hoàn toàn sau 3 lần retry, đẩy message vào Dead Letter Queue (DLQ): order-events-dlq.
     */
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            dltTopicSuffix = "-dlq"
    )
    @KafkaListener(topics = "order-events", groupId = "payment-service-group")
    @Transactional
    public void consumeOrderEvent(OrderEvent event) {
        if (event == null || event.getOrderId() == null) {
            log.warn("[PaymentService] Received empty or invalid OrderEvent from Kafka");
            return;
        }

        log.info("[PaymentService] Processing OrderEvent: type={}, orderId={}, code={}, method={}",
                event.getEventType(), event.getOrderId(), event.getOrderCode(), event.getPaymentMethod());

        if ("ORDER_CREATED".equalsIgnoreCase(event.getEventType())) {
            String method = event.getPaymentMethod();
            // Nếu đơn hàng thanh toán Online (SEPAY / VNPAY / WALLET)
            if ("ONLINE".equalsIgnoreCase(method) || "SEPAY".equalsIgnoreCase(method) || "VIETQR".equalsIgnoreCase(method)) {
                Optional<Transaction> existingTx = transactionRepository.findByOrderId(event.getOrderId());
                if (existingTx.isEmpty()) {
                    Transaction transaction = Transaction.builder()
                            .orderId(event.getOrderId())
                            .userId(event.getUserId())
                            .transactionType(TransactionType.ORDER_PAYMENT)
                            .paymentGateway(PaymentGateway.SEPAY)
                            .amount(event.getTotalAmount())
                            .currency("VND")
                            .status(TransactionStatus.PENDING)
                            .idempotencyKey("kafka_pay_" + event.getOrderId() + "_" + System.currentTimeMillis())
                            .createdAt(OffsetDateTime.now())
                            .build();

                    transactionRepository.save(transaction);
                    log.info("[PaymentService] Successfully recorded PENDING transaction for order #{} from Kafka", event.getOrderCode());
                } else {
                    log.info("[PaymentService] Transaction for order #{} already exists (idempotent skip)", event.getOrderCode());
                }
            }
        }
    }

    /**
     * DLT Handler: Hứng và xử lý các message bị thất bại hoàn toàn sau khi đã retry đủ 3 lần.
     * Đảm bảo message KHÔNG BAO GIỜ bị mất (chống thất thoát giao dịch khi payment sập).
     */
    @DltHandler
    public void handleDltOrderEvent(
            OrderEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String exceptionMessage) {
        log.error("🚨 [DLQ - PaymentService] Message moved to Dead Letter Queue! Topic: {}, OrderId: {}, Code: {}, Error: {}",
                topic,
                event != null ? event.getOrderId() : "null",
                event != null ? event.getOrderCode() : "null",
                exceptionMessage);
    }
}
