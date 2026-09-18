package com.fooddelivery.auth.entity;


import com.fooddelivery.auth.enums.UserRole;
import com.fooddelivery.auth.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Khóa chính (Auto-increment BIGINT)

    @Column(nullable = false, unique = true, length = 15)
    private String phone; // Số điện thoại là định danh chính

    @Column(unique = true, length = 255)
    private String email; // Email tùy chọn (dùng cho social login)

    @Column(name = "full_name", length = 255)
    private String fullName; // Họ và tên người dùng

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl; // URL CDN ảnh đại diện

    @Column(name = "password_hash", length = 255)
    private String passwordHash; // NULL nếu chỉ dùng OTP/social login

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role; // Phân quyền RBAC (CUSTOMER, SHOP_MANAGER, SHIPPER, ADMIN)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE; // Trạng thái tài khoản (ACTIVE, LOCKED, PENDING, DELETED)

    @Column(name = "area_id")
    private Long areaId; // FK tới areas.id (NULL nếu chưa xác thực khu)

    @Column(name = "is_area_verified")
    private Boolean isAreaVerified = false; // TRUE sau khi nhập đúng mã khu vực

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "default_address", columnDefinition = "jsonb")
    private Map<String, Object> defaultAddress; // Địa chỉ nhận hàng mặc định (JSONB)

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "saved_addresses", columnDefinition = "jsonb")
    private List<Map<String, Object>> savedAddresses; // Mảng các địa chỉ đã lưu (JSONB)

    @Column(name = "google_id", unique = true, length = 100)
    private String googleId; // Định danh Google OAuth

    @Column(name = "facebook_id", unique = true, length = 100)
    private String facebookId; // Định danh Facebook OAuth

    @Column(name = "refresh_token_hash", length = 255)
    private String refreshTokenHash; // Hash refresh token hiện tại

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt; // Thời gian đăng nhập gần nhất

    @Column(name = "failed_login_count")
    private Short failedLoginCount = 0; // Đếm số lần đăng nhập sai để khóa tạm

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt; // Thời điểm tạo bản ghi

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt; // Thời điểm cập nhật bản ghi gần nhất
}