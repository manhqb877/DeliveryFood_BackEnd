package com.fooddelivery.notification.dto.response;

import com.fooddelivery.notification.enums.MessageType;
import com.fooddelivery.notification.enums.SenderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private String id;
    private String conversationId;
    private SenderType senderType;
    private Long senderId;
    private MessageType messageType;
    private String content;
    private String attachmentUrl;
    private Boolean isRead;
    private Instant readAt;
    private Instant createdAt;
}
