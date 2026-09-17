package com.fooddelivery.notification.document;

import com.fooddelivery.notification.enums.NotificationChannel;
import com.fooddelivery.notification.enums.NotificationStatus;
import com.fooddelivery.notification.enums.NotificationType;
import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Map;

@Document(collection = "notification_logs")
@CompoundIndexes({
        @CompoundIndex(name = "idx_recipient_created", def = "{ 'recipientId': 1, 'createdAt': -1 }")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLogDocument {

    @Id
    private ObjectId id; // Khóa chính MongoDB ObjectId

    @Field("recipient_id")
    private Long recipientId; // ID người nhận (users.id, NULL nếu là Guest)

    @Field("recipient_phone")
    private String recipientPhone; // Số điện thoại dùng khi gửi SMS cho Guest

    @Field("channel")
    private NotificationChannel channel; // Kênh gửi (PUSH, SMS, EMAIL, IN_APP)

    @Field("notification_type")
    private NotificationType notificationType; // Loại thông báo (ORDER_STATUS, DELIVERY_OTP, v.v.)

    @Field("title")
    private String title; // Tiêu đề thông báo

    @Field("body")
    private String body; // Nội dung chi tiết thông báo

    @Field("data")
    private Map<String, Object> data; // Dữ liệu bổ sung tùy biến dạng JSON/Map

    @Field("status")
    private NotificationStatus status = NotificationStatus.SENT; // Trạng thái (SENT, DELIVERED, FAILED, READ)

    @Field("reference_id")
    @Indexed
    private Long referenceId; // order_id hoặc delivery_id liên quan

    @Field("created_at")
    @Indexed(expireAfterSeconds = 15552000) // TTL Index: tự động xóa log cũ hơn 6 tháng (180 ngày)
    private Instant createdAt = Instant.now();

    @Field("read_at")
    private Instant readAt; // Thời điểm người dùng đọc thông báo
}