package com.fooddelivery.notification.controller;

import com.fooddelivery.notification.document.NotificationLogDocument;
import com.fooddelivery.notification.dto.response.ApiResponse;
import com.fooddelivery.notification.dto.response.NotificationResponse;
import com.fooddelivery.notification.enums.NotificationStatus;
import com.fooddelivery.notification.repository.NotificationLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Quản lý thông báo người dùng và gian hàng")
public class NotificationController {

    private final NotificationLogRepository notificationLogRepository;

    @GetMapping("/recipient/{recipientId}")
    @Operation(summary = "Lấy danh sách thông báo theo người nhận (userId hoặc shopId)")
    public ResponseEntity<ApiResponse> getNotificationsByRecipient(@PathVariable Long recipientId) {
        List<NotificationLogDocument> docs = notificationLogRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
        List<NotificationResponse> responses = docs.stream().map(this::mapToResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Success")
                .data(responses)
                .build());
    }

    @GetMapping("/unread-count/{recipientId}")
    @Operation(summary = "Đếm số thông báo chưa đọc")
    public ResponseEntity<ApiResponse> getUnreadCount(@PathVariable Long recipientId) {
        long count = notificationLogRepository.countByRecipientIdAndReadAtIsNull(recipientId);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Success")
                .data(count)
                .build());
    }

    @PatchMapping("/recipient/{recipientId}/read-all")
    @Operation(summary = "Đánh dấu tất cả thông báo của người nhận là đã đọc")
    public ResponseEntity<ApiResponse> markAllAsRead(@PathVariable Long recipientId) {
        List<NotificationLogDocument> unreadDocs = notificationLogRepository.findByRecipientIdAndReadAtIsNull(recipientId);
        Instant now = Instant.now();
        unreadDocs.forEach(doc -> {
            doc.setReadAt(now);
            doc.setStatus(NotificationStatus.READ);
        });
        notificationLogRepository.saveAll(unreadDocs);
        log.info("Marked {} notifications as read for recipientId={}", unreadDocs.size(), recipientId);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Marked all as read")
                .data(unreadDocs.size())
                .build());
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Đánh dấu thông báo đã đọc")
    public ResponseEntity<ApiResponse> markAsRead(@PathVariable String id) {
        try {
            ObjectId objId = new ObjectId(id);
            return notificationLogRepository.findById(objId).map(doc -> {
                doc.setReadAt(Instant.now());
                doc.setStatus(NotificationStatus.READ);
                notificationLogRepository.save(doc);
                return ResponseEntity.ok(ApiResponse.builder()
                        .status(200)
                        .message("Marked as read")
                        .data(true)
                        .build());
            }).orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.builder()
                    .status(404)
                    .message("Notification not found")
                    .data(false)
                    .build()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.builder()
                    .status(400)
                    .message("Invalid ObjectId format")
                    .data(false)
                    .build());
        }
    }

    private NotificationResponse mapToResponse(NotificationLogDocument doc) {
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
