package com.fooddelivery.auth.enums;

import lombok.Getter;

@Getter
public enum ErrorCode {
    USER_NOT_FOUND(1001, "User not found"),
    INVALID_CREDENTIALS(1002, "Phone/Email or password is incorrect"),
    PHONE_ALREADY_EXISTS(1003, "Phone number is already registered"),
    EMAIL_ALREADY_EXISTS(1004, "Email is already registered"),
    ACCOUNT_LOCKED(1005, "Account is locked"),
    ACCOUNT_PENDING(1006, "Account is pending activation"),
    UNAUTHORIZED(1007, "Authentication required"),
    FORBIDDEN(1008, "Access denied"),
    VALIDATION_ERROR(1009, "Invalid request payload"),
    OTP_INVALID(1010, "Invalid OTP"),
    OTP_EXPIRED(1011, "OTP has expired"),
    INTERNAL_SERVER_ERROR(9999, "Internal server error");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
