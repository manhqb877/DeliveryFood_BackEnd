package com.fooddelivery.notification.document;

import com.fooddelivery.notification.enums.ConversationType;
import com.fooddelivery.notification.enums.SenderType;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document(collection = "conversations")
@CompoundIndexes({
        @CompoundIndex(name = "idx_participant_customer", def = "{ 'participants.customerId': 1 }"),
        @CompoundIndex(name = "idx_participant_shop", def = "{ 'participants.shopId': 1, 'last_message_at': -1 }")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationDocument {

    @Id
    private ObjectId id; // Khóa chính

    @Field("order_id")
    @Indexed
    private Long orderId; // ID đơn hàng liên quan từ order_db (nếu có)

    @Builder.Default
    @Field("conversation_type")
    private ConversationType conversationType = ConversationType.CUSTOMER_SHOP;

    @Field("participants")
    private Participants participants; // Danh sách các bên tham gia hội thoại

    // Snapshot thông tin hiển thị nhanh
    @Field("shop_name")
    private String shopName;

    @Field("shop_logo")
    private String shopLogo;

    @Field("customer_name")
    private String customerName;

    @Field("customer_phone")
    private String customerPhone;

    @Field("customer_avatar")
    private String customerAvatar;

    @Field("last_message_content")
    private String lastMessageContent;

    @Field("last_sender_type")
    private SenderType lastSenderType;

    @Field("last_message_at")
    private Instant lastMessageAt;

    @Builder.Default
    @Field("unread_shop_count")
    private Integer unreadShopCount = 0;

    @Builder.Default
    @Field("unread_customer_count")
    private Integer unreadCustomerCount = 0;

    @Builder.Default
    @Field("is_resolved")
    private Boolean isResolved = false;

    @Builder.Default
    @Field("created_at")
    private Instant createdAt = Instant.now();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Participants {
        @Field("customer_id")
        private Long customerId;

        @Field("guest_session_id")
        private Long guestSessionId;

        @Field("shop_id")
        private Long shopId;

        @Field("shipper_id")
        private Long shipperId;
    }
}