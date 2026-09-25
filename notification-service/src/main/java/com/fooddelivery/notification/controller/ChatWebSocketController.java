package com.fooddelivery.notification.controller;

import com.fooddelivery.notification.dto.request.SendMessageRequest;
import com.fooddelivery.notification.dto.response.MessageResponse;
import com.fooddelivery.notification.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;

    /**
     * Handle incoming STOMP message sent to /app/chat.send
     */
    @MessageMapping("/chat.send")
    public void handleSendMessage(@Payload SendMessageRequest request) {
        log.info("WebSocket received message from sender {}: conversationId={}", request.getSenderId(), request.getConversationId());
        try {
            MessageResponse response = chatService.sendMessage(request);
            log.debug("Message successfully processed and broadcasted: id={}", response.getId());
        } catch (Exception e) {
            log.error("Failed to process WebSocket message: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle incoming STOMP message sent to /app/chat.read
     * payload: { "conversationId": "...", "readerType": "SHOP" | "CUSTOMER" }
     */
    @MessageMapping("/chat.read")
    public void handleMarkRead(@Payload Map<String, String> payload) {
        String conversationId = payload.get("conversationId");
        String readerType = payload.get("readerType");
        if (conversationId != null && readerType != null) {
            log.info("WebSocket mark read: conversationId={}, readerType={}", conversationId, readerType);
            try {
                chatService.markMessagesAsRead(conversationId, readerType);
            } catch (Exception e) {
                log.error("Failed to mark as read via WebSocket: {}", e.getMessage(), e);
            }
        }
    }
}
