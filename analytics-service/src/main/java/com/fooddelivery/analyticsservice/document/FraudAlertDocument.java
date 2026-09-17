package com.fooddelivery.analyticsservice.document;


import com.fooddelivery.analyticsservice.enums.FraudAlertStatus;
import com.fooddelivery.analyticsservice.enums.FraudAlertType;
import com.fooddelivery.analyticsservice.enums.FraudSeverity;
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

@Document(collection = "fraud_alerts")
@CompoundIndexes({
        @CompoundIndex(name = "idx_user_created", def = "{ 'userId': 1, 'createdAt': -1 }"),
        @CompoundIndex(name = "idx_status_severity", def = "{ 'status': 1, 'severity': -1 }")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAlertDocument {

    @Id
    private ObjectId id; // Khóa chính MongoDB ObjectId

    @Field("alert_type")
    private FraudAlertType alertType; // Loại cảnh báo (PROMO_ABUSE, FAKE_ORDER, v.v.)

    @Field("severity")
    @Indexed
    private FraudSeverity severity; // Mức độ nghiêm trọng (LOW, MEDIUM, HIGH, CRITICAL)

    @Field("user_id")
    @Indexed
    private Long userId; // ID người dùng nghi vấn (nếu có)

    @Field("guest_session_id")
    private Long guestSessionId; // ID phiên guest (nếu có)

    @Field("ip_address")
    private String ipAddress; // Địa chỉ IP phát sinh hành vi

    @Field("device_fingerprint")
    private String deviceFingerprint; // Fingerprint thiết bị

    @Field("detected_lat")
    private Double detectedLat; // Vĩ độ phát hiện bất thường

    @Field("detected_lng")
    private Double detectedLng; // Kinh độ phát hiện bất thường

    @Field("details")
    private Map<String, Object> details; // Chi tiết phân tích hành vi gian lận dạng JSON/Map

    @Field("status")
    @Indexed
    private FraudAlertStatus status = FraudAlertStatus.OPEN; // Trạng thái xử lý cảnh báo

    @Field("reviewed_by")
    private Long reviewedBy; // ID Admin kiểm tra

    @Field("reviewed_at")
    private Instant reviewedAt; // Thời điểm kiểm tra

    @Field("review_note")
    private String reviewNote; // Ghi chú của Admin

    @Field("created_at")
    @Indexed
    private Instant createdAt = Instant.now(); // Thời điểm phát sinh cảnh báo
}
