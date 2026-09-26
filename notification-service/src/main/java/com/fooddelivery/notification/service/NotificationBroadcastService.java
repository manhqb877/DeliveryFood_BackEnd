package com.fooddelivery.notification.service;

import com.fooddelivery.notification.document.NotificationLogDocument;
import com.fooddelivery.notification.dto.response.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcast(NotificationLogDocument doc, String role) {
        if (doc == null) return;
        NotificationResponse response = mapToResponse(doc);

        try {
            Long recipientId = doc.getRecipientId();
            if (recipientId != null) {
                // Topic chung theo id: /topic/notifications.{recipientId}
                messagingTemplate.convertAndSend("/topic/notifications." + recipientId, response);

                // Topic theo role: /topic/notifications.{role}.{recipientId}
                if (role != null && !role.isBlank()) {
                    messagingTemplate.convertAndSend("/topic/notifications." + role.toLowerCase() + "." + recipientId, response);
                }
            }
            log.info("Broadcasted notification id={} to recipientId={} (role={})",
                    response.getId(), recipientId, role);
        } catch (Exception e) {
            log.warn("Failed to broadcast notification via WebSocket: {}", e.getMessage());
        }
    }

    public void broadcastToAdmin(NotificationLogDocument doc) {
        if (doc == null) return;
        NotificationResponse response = mapToResponse(doc);

        try {
            messagingTemplate.convertAndSend("/topic/notifications.admin", response);
            log.info("Broadcasted notification to admin topic: title={}", response.getTitle());
        } catch (Exception e) {
            log.warn("Failed to broadcast notification to admin: {}", e.getMessage());
        }
    }

    public NotificationResponse mapToResponse(NotificationLogDocument doc) {
        return NotificationResponse.builder()
                .id(doc.getId() != null ? doc.getId().toHexString() : null)
                .recipientId(doc.getRecipientId())
                .recipientPhone(doc.getRecipientPhone())
                .channel(doc.getChannel())
                .notificationType(doc.getNotificationType())
                .title(doc.getTitle())
                .body(doc.getBody())
                .data(doc.getData())
                .status(doc.getStatus())
                .referenceId(doc.getReferenceId())
                .createdAt(doc.getCreatedAt())
                .readAt(doc.getReadAt())
                .build();
    }
}
