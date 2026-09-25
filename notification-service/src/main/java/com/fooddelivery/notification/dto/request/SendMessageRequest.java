package com.fooddelivery.notification.dto.request;

import com.fooddelivery.notification.enums.MessageType;
import com.fooddelivery.notification.enums.SenderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {
    @NotBlank(message = "conversationId is required")
    private String conversationId;

    @NotNull(message = "senderType is required")
    private SenderType senderType;

    private Long senderId;

    @NotBlank(message = "content is required")
    private String content;

    private MessageType messageType;

    private String attachmentUrl;
}
