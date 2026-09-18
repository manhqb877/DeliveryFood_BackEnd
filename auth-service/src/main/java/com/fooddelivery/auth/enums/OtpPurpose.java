package com.fooddelivery.auth.enums;


public enum OtpPurpose {
    LOGIN,             // Đăng nhập
    REGISTER,          // Đăng ký tài khoản mới
    GUEST_VERIFY,      // Xác thực SĐT cho Guest session
    DELIVERY_CONFIRM,  // OTP xác nhận nhận hàng (Shipper nhập)
    RESET_PASSWORD     // Đặt lại mật khẩu
}