package com.fooddelivery.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "guest_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuestSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @Column(name = "session_token", nullable = false, unique = true, length = 64)
    private String sessionToken; // Token do server sinh, gửi cho client

    @Column(length = 15)
    private String phone; // Số điện thoại (sau khi xác thực OTP)

    @Column(name = "is_phone_verified")
    private Boolean isPhoneVerified = false; // Trạng thái xác thực SĐT của guest

    @Column(name = "order_count_today")
    private Short orderCountToday = 0; // Số đơn đã đặt hôm nay (chống spam)

    @Column(name = "order_value_today", precision = 12, scale = 2)
    private java.math.BigDecimal orderValueToday = java.math.BigDecimal.ZERO; // Tổng giá trị đơn hôm nay

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "delivery_address", columnDefinition = "jsonb")
    private Map<String, Object> deliveryAddress; // Địa chỉ nhập tay cho đơn hàng (JSONB)

    @Column(name = "upgraded_to_user_id")
    private Long upgradedToUserId; // FK tới users.id nếu đã nâng cấp thành tài khoản chính thức

    @Column(name = "device_fingerprint", length = 255)
    private String deviceFingerprint; // Fingerprint thiết bị (chống spam)

    @Column(name = "ip_address", length = 45)
    private String ipAddress; // Địa chỉ IP (IPv6-compatible)

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt; // Thời điểm hết hạn phiên (thường sau 24h)

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt; // Thời điểm tạo phiên
}