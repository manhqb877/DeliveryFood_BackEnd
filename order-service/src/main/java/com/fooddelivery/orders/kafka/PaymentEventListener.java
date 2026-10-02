package com.fooddelivery.orders.kafka;

import com.fooddelivery.orders.dto.event.PaymentEvent;
import com.fooddelivery.orders.service.OrderService;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final OrderService orderService;

    /**
     * Lắng nghe sự kiện payment-events từ payment-service.
     * Sử dụng @RetryableTopic:
     * - Tự động retry 3 lần với exponential backoff (1s, 2s, 4s).
     * - Nếu order-service bị sập khi payment bắn event, Kafka lưu message và order-service tự động xử lý khi bật lại.
     * - Nếu thất bại (database lỗi, conflict), chuyển vào Dead Letter Queue: payment-events-dlq.
     */
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            dltTopicSuffix = "-dlq"
    )
    @KafkaListener(topics = "payment-events", groupId = "order-service-payment-group")
    public void consumePaymentEvent(PaymentEvent event) {
        if (event == null || event.getOrderId() == null) {
            log.warn("[OrderService] Received empty or invalid PaymentEvent from Kafka");
            return;
        }

        log.info("[OrderService] Processing PaymentEvent from Kafka: type={}, orderId={}, code={}, status={}, amount={}",
                event.getEventType(), event.getOrderId(), event.getOrderCode(), event.getStatus(), event.getAmount());

        if ("PAYMENT_COMPLETED".equalsIgnoreCase(event.getEventType()) && "SUCCESS".equalsIgnoreCase(event.getStatus())) {
            try {
                orderService.markOrderAsPaid(
                        event.getOrderId(),
                        event.getGatewayTransactionId(),
                        event.getPaymentGateway() != null ? event.getPaymentGateway() : "SEPAY"
                );
                log.info("[OrderService] Successfully updated order #{} to PAID from Kafka PaymentEvent", event.getOrderId());
            } catch (Exception e) {
                log.error("[OrderService] Error marking order #{} as PAID: {}", event.getOrderId(), e.getMessage());
                throw e; // Ném lại để kích hoạt Kafka Retry và DLQ
            }
        }
    }

    /**
     * DLT Handler: Hứng các message thanh toán lỗi sau khi đã retry đủ 3 lần
     */
    @DltHandler
    public void handleDltPaymentEvent(
            PaymentEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String exceptionMessage) {
        log.error("🚨 [DLQ - OrderService] Payment message moved to Dead Letter Queue! Topic: {}, OrderId: {}, Code: {}, Error: {}",
                topic,
                event != null ? event.getOrderId() : "null",
                event != null ? event.getOrderCode() : "null",
                exceptionMessage);
    }
}
