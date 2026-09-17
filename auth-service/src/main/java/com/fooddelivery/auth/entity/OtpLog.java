package com.fooddelivery.auth.entity;


import com.fooddelivery.auth.enums.OtpPurpose;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "otp_logs", indexes = {
        @Index(name = "idx_otp_phone_purpose", columnList = "phone, purpose")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính

    @Column(nullable = false, length = 15)
    private String phone; // Số điện thoại nhận OTP

    @Column(name = "otp_hash", nullable = false, length = 255)
    private String otpHash; // Mã OTP đã được hash (không lưu plaintext)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OtpPurpose purpose; // Mục đích sử dụng OTP

    @Column(name = "is_used")
    private Boolean isUsed = false; // TRUE sau khi OTP được dùng thành công

    private Short attempts = 0; // Số lần nhập sai OTP này

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt; // Thời điểm hết hạn OTP (thường 5 phút)

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt; // Thời điểm tạo bản ghi OTP
}