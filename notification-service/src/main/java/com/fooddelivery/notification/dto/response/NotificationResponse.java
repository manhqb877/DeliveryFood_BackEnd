package com.fooddelivery.notification.dto.response;

import com.fooddelivery.notification.enums.NotificationChannel;
import com.fooddelivery.notification.enums.NotificationStatus;
import com.fooddelivery.notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private String id;
    private Long recipientId;
    private String recipientPhone;
    private NotificationChannel channel;
    private NotificationType notificationType;
    private String title;
    private String body;
    private Map<String, Object> data;
    private NotificationStatus status;
    private Long referenceId;
    private Instant createdAt;
    private Instant readAt;
}
