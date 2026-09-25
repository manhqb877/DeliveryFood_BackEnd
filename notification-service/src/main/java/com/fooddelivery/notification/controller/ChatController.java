package com.fooddelivery.notification.controller;

import com.fooddelivery.notification.dto.request.InitConversationRequest;
import com.fooddelivery.notification.dto.request.SendMessageRequest;
import com.fooddelivery.notification.dto.response.ApiResponse;
import com.fooddelivery.notification.dto.response.ConversationResponse;
import com.fooddelivery.notification.dto.response.MessageResponse;
import com.fooddelivery.notification.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Quản lý tin nhắn và hội thoại chat giữa Khách hàng và Quán ăn")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/conversations/init")
    @Operation(summary = "Khởi tạo hoặc lấy cuộc hội thoại giữa Khách hàng và Quán")
    public ResponseEntity<ApiResponse> initConversation(@Valid @RequestBody InitConversationRequest request) {
        ConversationResponse response = chatService.getOrCreateConversation(request);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Conversation initialized successfully")
                .data(response)
                .build());
    }

    @GetMapping("/conversations/{id}")
    @Operation(summary = "Lấy chi tiết cuộc hội thoại theo ID")
    public ResponseEntity<ApiResponse> getConversationById(@PathVariable String id) {
        ConversationResponse response = chatService.getConversationById(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Success")
                .data(response)
                .build());
    }

    @GetMapping("/conversations/shop/{shopId}")
    @Operation(summary = "Lấy danh sách các cuộc hội thoại của Quán ăn")
    public ResponseEntity<ApiResponse> getShopConversations(@PathVariable Long shopId) {
        List<ConversationResponse> list = chatService.getShopConversations(shopId);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Success")
                .data(list)
                .build());
    }

    @GetMapping("/conversations/customer")
    @Operation(summary = "Lấy danh sách các cuộc hội thoại của Khách hàng")
    public ResponseEntity<ApiResponse> getCustomerConversations(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long guestSessionId
    ) {
        List<ConversationResponse> list = chatService.getCustomerConversations(customerId, guestSessionId);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Success")
                .data(list)
                .build());
    }

    @GetMapping({"/conversations/{id}/messages", "/messages/conversation/{id}"})
    @Operation(summary = "Lấy lịch sử tin nhắn của cuộc hội thoại")
    public ResponseEntity<ApiResponse> getConversationMessages(@PathVariable String id) {
        List<MessageResponse> messages = chatService.getConversationMessages(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Success")
                .data(messages)
                .build());
    }

    @PostMapping("/messages")
    @Operation(summary = "Gửi tin nhắn mới (REST API)")
    public ResponseEntity<ApiResponse> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        MessageResponse response = chatService.sendMessage(request);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Message sent successfully")
                .data(response)
                .build());
    }

    @PatchMapping("/conversations/{id}/read")
    @Operation(summary = "Đánh dấu đã đọc tin nhắn trong cuộc hội thoại")
    public ResponseEntity<ApiResponse> markAsRead(
            @PathVariable String id,
            @RequestParam(defaultValue = "SHOP") String readerType
    ) {
        chatService.markMessagesAsRead(id, readerType);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Messages marked as read")
                .data(true)
                .build());
    }
}
