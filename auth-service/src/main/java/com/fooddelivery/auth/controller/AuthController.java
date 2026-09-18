package com.fooddelivery.auth.controller;

import com.fooddelivery.auth.common.ApiResponse;
import com.fooddelivery.auth.dto.request.LoginRequest;
import com.fooddelivery.auth.dto.request.RefreshTokenRequest;
import com.fooddelivery.auth.dto.request.RegisterRequest;
import com.fooddelivery.auth.dto.request.ResetPasswordRequest;
import com.fooddelivery.auth.dto.request.SendOtpRequest;
import com.fooddelivery.auth.dto.response.LoginResponse;
import com.fooddelivery.auth.dto.response.RegisterResponse;
import com.fooddelivery.auth.dto.response.UserResponse;
import com.fooddelivery.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/v1/auth/register/send-otp
     * Send a 6-digit OTP to the user's email for registration verification.
     */
    @PostMapping("/register/send-otp")
    public ResponseEntity<ApiResponse<Void>> sendRegisterOtp(
            @Valid @RequestBody SendOtpRequest request
    ) {
        authService.sendRegisterOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Registration OTP sent successfully to email", null));
    }

    /**
     * POST /api/v1/auth/register/verify-otp
     * Verify OTP before proceeding to registration.
     */
    @PostMapping("/register/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyRegisterOtp(
            @Valid @RequestBody com.fooddelivery.auth.dto.request.VerifyOtpRequest request
    ) {
        authService.verifyRegisterOtp(request);
        return ResponseEntity.ok(ApiResponse.success("OTP verified successfully", null));
    }

    /**
     * POST /api/v1/auth/register
     * Register a new user account (CUSTOMER, SHOP_MANAGER, SHIPPER).
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Registration successful", response));
    }

    /**
     * POST /api/v1/auth/login
     * Authenticate with phone + password, returns access & refresh tokens.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    /**
     * POST /api/v1/auth/refresh
     * Get a new access token using a valid refresh token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        LoginResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    /**
     * POST /api/v1/auth/logout
     * Invalidate the current access token (blacklist in Redis).
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String token = (authHeader != null && authHeader.startsWith("Bearer "))
                ? authHeader.substring(7)
                : authHeader;
        authService.logout(token, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    /**
     * GET /api/v1/auth/me
     * Get the profile of the currently authenticated user.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UserResponse response = authService.getMyProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", response));
    }

    /**
     * POST /api/v1/auth/forgot-password/send-otp
     * Send a 6-digit OTP to the user's email for password reset.
     */
    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<ApiResponse<Void>> sendForgotPasswordOtp(
            @Valid @RequestBody SendOtpRequest request
    ) {
        authService.sendForgotPasswordOtp(request);
        return ResponseEntity.ok(ApiResponse.success("OTP sent successfully to email", null));
    }

    /**
     * POST /api/v1/auth/forgot-password/verify-otp
     * Verify OTP before allowing the user to reset password.
     */
    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyForgotPasswordOtp(
            @Valid @RequestBody com.fooddelivery.auth.dto.request.VerifyOtpRequest request
    ) {
        authService.verifyForgotPasswordOtp(request);
        return ResponseEntity.ok(ApiResponse.success("OTP verified successfully", null));
    }

    /**
     * POST /api/v1/auth/forgot-password/reset
     * Verify OTP and set a new password.
     */
    @PostMapping("/forgot-password/reset")
    public ResponseEntity<ApiResponse<Void>> resetPasswordWithOtp(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        authService.resetPasswordWithOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
    }
}
