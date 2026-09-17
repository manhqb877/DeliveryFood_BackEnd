package com.fooddelivery.auth.enums;

public enum UserStatus {
    ACTIVE,   // Đang hoạt động bình thường
    LOCKED,   // Bị khóa (Admin khóa thủ công hoặc vi phạm chính sách)
    PENDING,  // Chờ xác thực OTP lần đầu sau đăng ký
    DELETED   // Xóa mềm — giữ dữ liệu nhưng không cho đăng nhập
}