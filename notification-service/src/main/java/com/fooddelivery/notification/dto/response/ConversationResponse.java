package com.fooddelivery.notification.dto.response;

import com.fooddelivery.notification.enums.ConversationType;
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
public class ConversationResponse {
    private String id;
    private Long orderId;
    private ConversationType conversationType;
    private Long shopId;
    private String shopName;
    private String shopLogo;
    private Long customerId;
    private Long guestSessionId;
    private String customerName;
    private String customerPhone;
    private String customerAvatar;
    private String lastMessageContent;
    private SenderType lastSenderType;
    private Instant lastMessageAt;
    private Integer unreadShopCount;
    private Integer unreadCustomerCount;
    private Boolean isResolved;
    private Instant createdAt;
}
