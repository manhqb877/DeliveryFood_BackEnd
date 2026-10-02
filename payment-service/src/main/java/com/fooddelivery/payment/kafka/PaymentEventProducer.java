package com.fooddelivery.payment.kafka;

import com.fooddelivery.payment.dto.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    public static final String TOPIC_PAYMENT_EVENTS = "payment-events";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPaymentCompleted(PaymentEvent event) {
        try {
            log.info("Publishing PAYMENT_COMPLETED event for orderId: {}, code: {} to topic: {}",
                    event.getOrderId(), event.getOrderCode(), TOPIC_PAYMENT_EVENTS);

            kafkaTemplate.send(TOPIC_PAYMENT_EVENTS, String.valueOf(event.getOrderId()), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Successfully published PAYMENT_COMPLETED event for order #{} (partition={}, offset={})",
                                    event.getOrderCode(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Failed to publish PAYMENT_COMPLETED event to Kafka: {}", ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Error dispatching PAYMENT_COMPLETED event to Kafka: {}", e.getMessage(), e);
        }
    }
}
