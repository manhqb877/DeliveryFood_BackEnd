package com.fooddelivery.notification.kafka;

import com.fooddelivery.notification.document.NotificationLogDocument;
import com.fooddelivery.notification.dto.event.UserEvent;
import com.fooddelivery.notification.enums.NotificationChannel;
import com.fooddelivery.notification.enums.NotificationStatus;
import com.fooddelivery.notification.enums.NotificationType;
import com.fooddelivery.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumer {

    private final NotificationLogRepository notificationLogRepository;

    @KafkaListener(topics = "user-events", groupId = "notification-service-group")
    public void consumeUserEvent(UserEvent event) {
        if (event == null) {
            log.warn("Received empty or null UserEvent from Kafka");
            return;
        }

        log.info("Received Kafka UserEvent: type={}, userId={}, phone={}, action={}",
                event.getEventType(), event.getUserId(), event.getPhone(), event.getAction());

        try {
            if (event.getUserId() != null) {
                String title;
                String body;

                if ("LOCKED".equalsIgnoreCase(event.getStatus())) {
                    title = "Tài khoản bị khóa";
                    body = "Tài khoản của bạn đã bị tạm khóa bởi Quản trị viên hệ thống. Vui lòng liên hệ hỗ trợ.";
                } else if ("PASSWORD_RESET".equalsIgnoreCase(event.getAction())) {
                    title = "Mật khẩu đã được cấp lại";
                    body = "Quản trị viên đã cấp lại mật khẩu cho tài khoản của bạn. Vui lòng kiểm tra và đổi lại mật khẩu.";
                } else {
                    title = "Cập nhật hồ sơ tài khoản";
                    body = "Thông tin tài khoản của bạn (" + (event.getFullName() != null ? event.getFullName() : event.getPhone()) + ") đã được cập nhật thành công.";
                }

                Map<String, Object> data = new HashMap<>();
                data.put("userId", event.getUserId());
                data.put("action", event.getAction());
                data.put("role", event.getRole());
                data.put("status", event.getStatus());

                NotificationLogDocument userNotification = NotificationLogDocument.builder()
                        .recipientId(event.getUserId())
                        .recipientPhone(event.getPhone())
                        .channel(NotificationChannel.IN_APP)
                        .notificationType(NotificationType.SYSTEM)
                        .title(title)
                        .body(body)
                        .data(data)
                        .status(NotificationStatus.SENT)
                        .referenceId(event.getUserId())
                        .createdAt(Instant.now())
                        .build();

                notificationLogRepository.save(userNotification);
                log.info("Saved user account notification for userId={}", event.getUserId());
            }
        } catch (Exception e) {
            log.error("Error processing UserEvent: {}", e.getMessage(), e);
        }
    }
}
