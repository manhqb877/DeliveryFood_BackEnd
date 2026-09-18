package com.fooddelivery.notification.document;


import com.fooddelivery.notification.enums.ConversationType;
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
        @CompoundIndex(name = "idx_participant_customer", def = "{ 'participants.customerId': 1 }")
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
    private Long orderId; // ID đơn hàng liên quan từ order_db

    @Field("conversation_type")
    private ConversationType conversationType; // Phân loại luồng chat (CUSTOMER_SHOP, CUSTOMER_SHIPPER, v.v.)

    @Field("participants")
    private Participants participants; // Danh sách các bên tham gia hội thoại

    @Field("last_message_at")
    private Instant lastMessageAt; // Thời điểm có tin nhắn mới nhất

    @Field("is_resolved")
    private Boolean isResolved = false; // Trạng thái đã giải quyết xong hội thoại hỗ trợ chưa

    @Field("created_at")
    private Instant createdAt = Instant.now();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Participants {
        @Field("customer_id")
        private Long customerId; // ID Khách hàng (NULL nếu là Guest)

        @Field("guest_session_id")
        private Long guestSessionId; // ID Guest session

        @Field("shop_id")
        private Long shopId; // ID gian hàng

        @Field("shipper_id")
        private Long shipperId; // ID Shipper
    }
}