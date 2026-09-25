package com.fooddelivery.notification.dto.request;

import com.fooddelivery.notification.enums.ConversationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitConversationRequest {
    private Long shopId;
    private Long customerId;
    private Long guestSessionId;
    private Long orderId;
    private ConversationType conversationType;
    private String shopName;
    private String shopLogo;
    private String customerName;
    private String customerPhone;
    private String customerAvatar;
}
