package com.fooddelivery.notification.service;

import com.fooddelivery.notification.dto.request.InitConversationRequest;
import com.fooddelivery.notification.dto.request.SendMessageRequest;
import com.fooddelivery.notification.dto.response.ConversationResponse;
import com.fooddelivery.notification.dto.response.MessageResponse;

import java.util.List;

public interface ChatService {
    ConversationResponse getOrCreateConversation(InitConversationRequest request);
    ConversationResponse getConversationById(String conversationId);
    List<ConversationResponse> getShopConversations(Long shopId);
    List<ConversationResponse> getCustomerConversations(Long customerId, Long guestSessionId);
    List<MessageResponse> getConversationMessages(String conversationId);
    MessageResponse sendMessage(SendMessageRequest request);
    void markMessagesAsRead(String conversationId, String readerType);
}
