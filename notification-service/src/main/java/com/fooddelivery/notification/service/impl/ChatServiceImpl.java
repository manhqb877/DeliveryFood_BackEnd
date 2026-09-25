package com.fooddelivery.notification.service.impl;

import com.fooddelivery.notification.converter.ChatConverter;
import com.fooddelivery.notification.document.ConversationDocument;
import com.fooddelivery.notification.document.MessageDocument;
import com.fooddelivery.notification.dto.request.InitConversationRequest;
import com.fooddelivery.notification.dto.request.SendMessageRequest;
import com.fooddelivery.notification.dto.response.ConversationResponse;
import com.fooddelivery.notification.dto.response.MessageResponse;
import com.fooddelivery.notification.enums.ConversationType;
import com.fooddelivery.notification.enums.MessageType;
import com.fooddelivery.notification.enums.SenderType;
import com.fooddelivery.notification.repository.ConversationRepository;
import com.fooddelivery.notification.repository.MessageRepository;
import com.fooddelivery.notification.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ChatConverter chatConverter;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public ConversationResponse getOrCreateConversation(InitConversationRequest request) {
        if (request.getShopId() == null) {
            throw new IllegalArgumentException("shopId is required to start a chat");
        }

        Optional<ConversationDocument> existing = Optional.empty();

        if (request.getOrderId() != null && request.getCustomerId() != null) {
            existing = conversationRepository.findFirstByParticipantsShopIdAndParticipantsCustomerIdAndOrderId(
                    request.getShopId(), request.getCustomerId(), request.getOrderId());
        }

        if (existing.isEmpty() && request.getCustomerId() != null) {
            existing = conversationRepository.findFirstByParticipantsShopIdAndParticipantsCustomerId(
                    request.getShopId(), request.getCustomerId());
        }

        if (existing.isEmpty() && request.getGuestSessionId() != null) {
            existing = conversationRepository.findFirstByParticipantsShopIdAndParticipantsGuestSessionId(
                    request.getShopId(), request.getGuestSessionId());
        }

        ConversationDocument conv;
        if (existing.isPresent()) {
            conv = existing.get();
            boolean updated = false;

            if (request.getShopName() != null && !request.getShopName().equals(conv.getShopName())) {
                conv.setShopName(request.getShopName());
                updated = true;
            }
            if (request.getShopLogo() != null && !request.getShopLogo().equals(conv.getShopLogo())) {
                conv.setShopLogo(request.getShopLogo());
                updated = true;
            }
            if (request.getCustomerName() != null && !request.getCustomerName().equals(conv.getCustomerName())) {
                conv.setCustomerName(request.getCustomerName());
                updated = true;
            }
            if (request.getCustomerPhone() != null && !request.getCustomerPhone().equals(conv.getCustomerPhone())) {
                conv.setCustomerPhone(request.getCustomerPhone());
                updated = true;
            }
            if (request.getCustomerAvatar() != null && !request.getCustomerAvatar().equals(conv.getCustomerAvatar())) {
                conv.setCustomerAvatar(request.getCustomerAvatar());
                updated = true;
            }

            if (updated) {
                conv = conversationRepository.save(conv);
            }
        } else {
            String defaultShopName = request.getShopName() != null ? request.getShopName() : ("Gian hàng #" + request.getShopId());
            String defaultCustomerName = request.getCustomerName() != null ? request.getCustomerName()
                    : (request.getCustomerId() != null ? "Khách hàng #" + request.getCustomerId() : "Khách vãng lai");

            conv = ConversationDocument.builder()
                    .orderId(request.getOrderId())
                    .conversationType(request.getConversationType() != null ? request.getConversationType() : ConversationType.CUSTOMER_SHOP)
                    .participants(ConversationDocument.Participants.builder()
                            .shopId(request.getShopId())
                            .customerId(request.getCustomerId())
                            .guestSessionId(request.getGuestSessionId())
                            .build())
                    .shopName(defaultShopName)
                    .shopLogo(request.getShopLogo())
                    .customerName(defaultCustomerName)
                    .customerPhone(request.getCustomerPhone())
                    .customerAvatar(request.getCustomerAvatar())
                    .lastMessageContent(null)
                    .lastSenderType(null)
                    .lastMessageAt(null)
                    .unreadShopCount(0)
                    .unreadCustomerCount(0)
                    .isResolved(false)
                    .createdAt(Instant.now())
                    .build();

            conv = conversationRepository.save(conv);
            log.info("Created new conversation id={} between shopId={} and customerId={}",
                    conv.getId(), request.getShopId(), request.getCustomerId());
        }

        return chatConverter.toConversationResponse(conv);
    }

    @Override
    public ConversationResponse getConversationById(String conversationId) {
        ObjectId objId = new ObjectId(conversationId);
        ConversationDocument doc = conversationRepository.findById(objId)
                .orElseThrow(() -> new NoSuchElementException("Conversation not found with id: " + conversationId));
        return chatConverter.toConversationResponse(doc);
    }

    @Override
    public List<ConversationResponse> getShopConversations(Long shopId) {
        if (shopId == null) return Collections.emptyList();
        List<ConversationDocument> list = conversationRepository.findByParticipantsShopIdOrderByLastMessageAtDesc(shopId);
        return list.stream().map(chatConverter::toConversationResponse).collect(Collectors.toList());
    }

    @Override
    public List<ConversationResponse> getCustomerConversations(Long customerId, Long guestSessionId) {
        List<ConversationDocument> list = Collections.emptyList();
        if (customerId != null) {
            list = conversationRepository.findByParticipantsCustomerIdOrderByLastMessageAtDesc(customerId);
        } else if (guestSessionId != null) {
            list = conversationRepository.findByParticipantsGuestSessionIdOrderByLastMessageAtDesc(guestSessionId);
        }
        return list.stream().map(chatConverter::toConversationResponse).collect(Collectors.toList());
    }

    @Override
    public List<MessageResponse> getConversationMessages(String conversationId) {
        ObjectId objId = new ObjectId(conversationId);
        List<MessageDocument> list = messageRepository.findByConversationIdOrderByCreatedAtAsc(objId);
        return list.stream().map(chatConverter::toMessageResponse).collect(Collectors.toList());
    }

    @Override
    public MessageResponse sendMessage(SendMessageRequest request) {
        ObjectId convId = new ObjectId(request.getConversationId());
        ConversationDocument conv = conversationRepository.findById(convId)
                .orElseThrow(() -> new NoSuchElementException("Conversation not found with id: " + request.getConversationId()));

        boolean isCustomerSender = request.getSenderType() == SenderType.CUSTOMER || request.getSenderType() == SenderType.GUEST;
        boolean shouldAutoReply = false;

        if (isCustomerSender) {
            boolean hasShopEverReplied = messageRepository.findFirstByConversationIdAndSenderTypeOrderByCreatedAtDesc(convId, SenderType.SHOP).isPresent();
            if (!hasShopEverReplied) {
                // Quán chưa từng phản hồi hoặc chưa từng có tin nhắn tự động nào trong cuộc trò chuyện này
                shouldAutoReply = true;
                log.info("Auto-reply triggered: no previous shop messages in conversation id={}", convId);
            } else {
                Optional<MessageDocument> lastMsgOpt = messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(convId);
                if (lastMsgOpt.isPresent()) {
                    Instant lastTime = lastMsgOpt.get().getCreatedAt();
                    long minutesSinceLastMsg = lastTime != null ? Duration.between(lastTime, Instant.now()).toMinutes() : 999;
                    if (minutesSinceLastMsg >= 60) {
                        // Khách hàng nhắn lại sau >= 1 giờ (60 phút) -> Tự động gửi phản hồi
                        shouldAutoReply = true;
                        log.info("Auto-reply triggered: {} minutes since last message in conversation id={}", minutesSinceLastMsg, convId);
                    }
                }
            }
        }

        MessageDocument message = MessageDocument.builder()
                .conversationId(convId)
                .senderType(request.getSenderType())
                .senderId(request.getSenderId())
                .messageType(request.getMessageType() != null ? request.getMessageType() : MessageType.TEXT)
                .content(request.getContent().trim())
                .attachmentUrl(request.getAttachmentUrl())
                .isRead(false)
                .createdAt(Instant.now())
                .build();

        message = messageRepository.save(message);

        // Cập nhật Conversation snapshot
        conv.setLastMessageContent(message.getContent());
        conv.setLastSenderType(message.getSenderType());
        conv.setLastMessageAt(message.getCreatedAt());

        if (request.getSenderType() == SenderType.CUSTOMER || request.getSenderType() == SenderType.GUEST) {
            conv.setUnreadShopCount((conv.getUnreadShopCount() != null ? conv.getUnreadShopCount() : 0) + 1);
        } else if (request.getSenderType() == SenderType.SHOP) {
            conv.setUnreadCustomerCount((conv.getUnreadCustomerCount() != null ? conv.getUnreadCustomerCount() : 0) + 1);
        }

        conversationRepository.save(conv);

        MessageResponse response = chatConverter.toMessageResponse(message);

        // Realtime Broadcast qua STOMP WebSocket
        try {
            // 1. Kênh phòng chat riêng (cả 2 bên đang mở phòng chat này)
            messagingTemplate.convertAndSend("/topic/conversation." + request.getConversationId(), response);

            // 2. Kênh thông báo của Quán (để cập nhật danh sách chat hoặc hiện notification)
            if (conv.getParticipants() != null && conv.getParticipants().getShopId() != null) {
                messagingTemplate.convertAndSend("/topic/shop." + conv.getParticipants().getShopId(), response);
            }

            // 3. Kênh thông báo của Khách hàng
            if (conv.getParticipants() != null && conv.getParticipants().getCustomerId() != null) {
                messagingTemplate.convertAndSend("/topic/customer." + conv.getParticipants().getCustomerId(), response);
            }
        } catch (Exception e) {
            log.warn("Failed to broadcast message via WebSocket: {}", e.getMessage());
        }

        // Tự động gửi tin nhắn chào mừng/phản hồi tự động từ Quán nếu khách nhắn lại sau >= 1 giờ
        if (shouldAutoReply) {
            final String targetConversationId = request.getConversationId();
            final Long shopId = conv.getParticipants() != null ? conv.getParticipants().getShopId() : null;
            final Long customerId = conv.getParticipants() != null ? conv.getParticipants().getCustomerId() : null;
            final Long guestSessionId = conv.getParticipants() != null ? conv.getParticipants().getGuestSessionId() : null;
            final String shopName = conv.getShopName() != null ? conv.getShopName() : "quán";

            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(700); // Khoảng dừng ngắn tạo cảm giác phản hồi tự động tự nhiên
                    sendAutoReplyMessage(targetConversationId, shopId, customerId, guestSessionId, shopName);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    log.error("Failed to execute auto-reply: {}", e.getMessage(), e);
                }
            });
        }

        return response;
    }

    private void sendAutoReplyMessage(String conversationIdStr, Long shopId, Long customerId, Long guestSessionId, String shopName) {
        ObjectId convId = new ObjectId(conversationIdStr);
        Optional<ConversationDocument> convOpt = conversationRepository.findById(convId);
        if (convOpt.isEmpty()) return;
        ConversationDocument conv = convOpt.get();

        String autoReplyContent = "Dạ chào bạn! Rất vui được đón tiếp bạn đến với " + shopName + ". Cảm ơn bạn đã nhắn tin cho quán. Quán đã nhận được tin nhắn và sẽ phản hồi cho bạn ngay trong giây lát nhé! Nếu bạn cần hỗ trợ nhanh về món ăn hoặc đơn hàng, bạn vui lòng để lại yêu cầu cụ thể tại đây ạ. Chúc bạn một ngày thật vui vẻ và ngon miệng! ❤️";

        MessageDocument autoReply = MessageDocument.builder()
                .conversationId(convId)
                .senderType(SenderType.SHOP)
                .senderId(shopId != null ? shopId : 0L)
                .messageType(MessageType.TEXT)
                .content(autoReplyContent)
                .isRead(false)
                .createdAt(Instant.now())
                .build();

        autoReply = messageRepository.save(autoReply);

        // Cập nhật Conversation snapshot
        conv.setLastMessageContent(autoReply.getContent());
        conv.setLastSenderType(SenderType.SHOP);
        conv.setLastMessageAt(autoReply.getCreatedAt());
        conv.setUnreadCustomerCount((conv.getUnreadCustomerCount() != null ? conv.getUnreadCustomerCount() : 0) + 1);
        conversationRepository.save(conv);

        MessageResponse autoReplyResponse = chatConverter.toMessageResponse(autoReply);

        // Realtime Broadcast tin nhắn tự động qua STOMP WebSocket
        try {
            messagingTemplate.convertAndSend("/topic/conversation." + conversationIdStr, autoReplyResponse);

            Long targetCustomerTopic = customerId != null ? customerId : guestSessionId;
            if (targetCustomerTopic != null) {
                messagingTemplate.convertAndSend("/topic/customer." + targetCustomerTopic, autoReplyResponse);
            }
            log.info("Auto-reply sent successfully to conversationId={}", conversationIdStr);
        } catch (Exception e) {
            log.warn("Failed to broadcast auto-reply via WebSocket: {}", e.getMessage());
        }
    }

    @Override
    public void markMessagesAsRead(String conversationId, String readerType) {
        try {
            ObjectId convId = new ObjectId(conversationId);
            Optional<ConversationDocument> convOpt = conversationRepository.findById(convId);
            if (convOpt.isPresent()) {
                ConversationDocument conv = convOpt.get();
                if ("SHOP".equalsIgnoreCase(readerType)) {
                    conv.setUnreadShopCount(0);
                } else {
                    conv.setUnreadCustomerCount(0);
                }
                conversationRepository.save(conv);
            }

            List<MessageDocument> unreadList = messageRepository.findByConversationIdAndIsReadFalse(convId);
            for (MessageDocument msg : unreadList) {
                msg.setIsRead(true);
                msg.setReadAt(Instant.now());
            }
            if (!unreadList.isEmpty()) {
                messageRepository.saveAll(unreadList);
            }

            // Bắn tín hiệu đã đọc qua WebSocket
            messagingTemplate.convertAndSend("/topic/conversation." + conversationId + ".read",
                    Map.of("conversationId", conversationId, "readerType", readerType));
        } catch (Exception e) {
            log.warn("Failed to mark messages as read for conversationId={}: {}", conversationId, e.getMessage());
        }
    }
}
