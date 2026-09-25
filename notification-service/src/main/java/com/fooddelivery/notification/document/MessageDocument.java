package com.fooddelivery.notification.document;


import com.fooddelivery.notification.enums.MessageType;
import com.fooddelivery.notification.enums.SenderType;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document(collection = "messages")
@CompoundIndexes({
        @CompoundIndex(name = "idx_conversation_created", def = "{ 'conversationId': 1, 'createdAt': 1 }")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDocument {

    @Id
    private ObjectId id; // Khóa chính

    @Field("conversation_id")
    private ObjectId conversationId; // Tham chiếu tới _id của ConversationDocument

    @Field("sender_type")
    private SenderType senderType; // Tác nhân gửi (CUSTOMER, GUEST, SHOP, SHIPPER, ADMIN, BOT)

    @Field("sender_id")
    private Long senderId; // ID cụ thể của người gửi

    @Builder.Default
    @Field("message_type")
    private MessageType messageType = MessageType.TEXT; // Loại tin nhắn (TEXT, IMAGE, ORDER_LINK, SYSTEM)

    @Field("content")
    private String content; // Nội dung tin nhắn văn bản

    @Field("attachment_url")
    private String attachmentUrl; // Đường dẫn file/ảnh đính kèm nếu có

    @Builder.Default
    @Field("is_read")
    private Boolean isRead = false; // Trạng thái đã đọc tin nhắn

    @Field("read_at")
    private Instant readAt; // Thời điểm đọc

    @Builder.Default
    @Field("created_at")
    private Instant createdAt = Instant.now(); // Thời điểm gửi tin nhắn
}