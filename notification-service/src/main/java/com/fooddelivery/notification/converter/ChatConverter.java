package com.fooddelivery.notification.converter;

import com.fooddelivery.notification.document.ConversationDocument;
import com.fooddelivery.notification.document.MessageDocument;
import com.fooddelivery.notification.dto.response.ConversationResponse;
import com.fooddelivery.notification.dto.response.MessageResponse;
import org.springframework.stereotype.Component;

@Component
public class ChatConverter {

    public MessageResponse toMessageResponse(MessageDocument doc) {
        if (doc == null) return null;
        return MessageResponse.builder()
                .id(doc.getId() != null ? doc.getId().toHexString() : null)
                .conversationId(doc.getConversationId() != null ? doc.getConversationId().toHexString() : null)
                .senderType(doc.getSenderType())
                .senderId(doc.getSenderId())
                .messageType(doc.getMessageType())
                .content(doc.getContent())
                .attachmentUrl(doc.getAttachmentUrl())
                .isRead(doc.getIsRead())
                .readAt(doc.getReadAt())
                .createdAt(doc.getCreatedAt())
                .build();
    }

    public ConversationResponse toConversationResponse(ConversationDocument doc) {
        if (doc == null) return null;

        Long customerId = null;
        Long guestSessionId = null;
        Long shopId = null;

        if (doc.getParticipants() != null) {
            customerId = doc.getParticipants().getCustomerId();
            guestSessionId = doc.getParticipants().getGuestSessionId();
            shopId = doc.getParticipants().getShopId();
        }

        return ConversationResponse.builder()
                .id(doc.getId() != null ? doc.getId().toHexString() : null)
                .orderId(doc.getOrderId())
                .conversationType(doc.getConversationType())
                .shopId(shopId)
                .shopName(doc.getShopName())
                .shopLogo(doc.getShopLogo())
                .customerId(customerId)
                .guestSessionId(guestSessionId)
                .customerName(doc.getCustomerName())
                .customerPhone(doc.getCustomerPhone())
                .customerAvatar(doc.getCustomerAvatar())
                .lastMessageContent(doc.getLastMessageContent())
                .lastSenderType(doc.getLastSenderType())
                .lastMessageAt(doc.getLastMessageAt() != null ? doc.getLastMessageAt() : doc.getCreatedAt())
                .unreadShopCount(doc.getUnreadShopCount() != null ? doc.getUnreadShopCount() : 0)
                .unreadCustomerCount(doc.getUnreadCustomerCount() != null ? doc.getUnreadCustomerCount() : 0)
                .isResolved(doc.getIsResolved())
                .createdAt(doc.getCreatedAt())
                .build();
    }
}
