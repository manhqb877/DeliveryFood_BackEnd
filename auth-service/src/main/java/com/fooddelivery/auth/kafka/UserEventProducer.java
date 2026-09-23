package com.fooddelivery.auth.kafka;

import com.fooddelivery.auth.dto.event.UserEvent;
import com.fooddelivery.auth.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    public static final String TOPIC_USER_EVENTS = "user-events";

    public void publishUserUpdated(User user, String action) {
        try {
            UserEvent event = UserEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType("USER_UPDATED")
                    .userId(user.getId())
                    .phone(user.getPhone())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .role(user.getRole() != null ? user.getRole().name() : null)
                    .status(user.getStatus() != null ? user.getStatus().name() : null)
                    .areaId(user.getAreaId())
                    .action(action != null ? action : "PROFILE_UPDATED")
                    .timestamp(Instant.now())
                    .build();

            log.info("Publishing USER_UPDATED event for userId={} to Kafka topic '{}'", user.getId(), TOPIC_USER_EVENTS);

            kafkaTemplate.send(TOPIC_USER_EVENTS, String.valueOf(user.getId()), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Successfully published USER_UPDATED event for userId={} to partition {} offset {}",
                                    user.getId(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.warn("Failed to publish USER_UPDATED event to Kafka: {}", ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.warn("Could not dispatch USER_UPDATED event to Kafka broker: {}", e.getMessage());
        }
    }
}
