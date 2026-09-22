package com.fooddelivery.auth.service.impl;

import com.fooddelivery.auth.converter.UserConverter;
import com.fooddelivery.auth.dto.request.LoginRequest;
import com.fooddelivery.auth.dto.request.RefreshTokenRequest;
import com.fooddelivery.auth.dto.request.RegisterRequest;
import com.fooddelivery.auth.dto.request.ResetPasswordRequest;
import com.fooddelivery.auth.dto.request.SendOtpRequest;
import com.fooddelivery.auth.dto.response.LoginResponse;
import com.fooddelivery.auth.dto.response.RegisterResponse;
import com.fooddelivery.auth.dto.response.UserResponse;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.enums.ErrorCode;
import com.fooddelivery.auth.enums.UserStatus;
import com.fooddelivery.auth.exception.BusinessException;
import com.fooddelivery.auth.repository.UserRepository;
import com.fooddelivery.auth.security.JwtProvider;
import com.fooddelivery.auth.service.AuthService;
import com.fooddelivery.auth.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String BLACKLIST_PREFIX = "blacklist:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh:";
    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final UserConverter userConverter;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;
    private final com.fooddelivery.auth.repository.ShipperProfileRepository shipperProfileRepository;
    private final com.fooddelivery.auth.repository.UserAddressRepository userAddressRepository;

    /**
     * Register a new user account.
     */
    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        log.info("Registration attempt for phone: {}", request.getPhone());

        validatePhoneNotTaken(request.getPhone());
        validateEmailNotTaken(request.getEmail());

        // Verify OTP from Redis
        String email = request.getEmail().trim().toLowerCase();
        String storedOtp = redisTemplate.opsForValue().get("otp:register:" + email);
        if (storedOtp == null) {
            throw new BusinessException(ErrorCode.OTP_EXPIRED);
        }
        if (!storedOtp.equals(request.getOtp())) {
            throw new BusinessException(ErrorCode.OTP_INVALID);
        }

        User user = userConverter.toEntity(request);
        User savedUser = userRepository.save(user);

        // Delete OTP from Redis
        redisTemplate.delete("otp:register:" + email);

        log.info("Registered successfully for phone: {}", request.getPhone());

        return RegisterResponse.builder()
                .user(userConverter.toResponse(savedUser))
                .message("Registration successful")
                .build();
    }

    @Override
    @Transactional
    public RegisterResponse registerShipper(com.fooddelivery.auth.dto.request.RegisterShipperRequest request) {
        log.info("Shipper registration attempt for phone: {}", request.getPhone());

        validatePhoneNotTaken(request.getPhone());
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            validateEmailNotTaken(request.getEmail());
        }

        // Verify OTP from Redis (using phone number instead of email for shipper OTP since email is optional)
        String identifier = (request.getEmail() != null && !request.getEmail().isEmpty()) ? request.getEmail().trim().toLowerCase() : request.getPhone();
        String storedOtp = redisTemplate.opsForValue().get("otp:register:" + identifier);
        
        if (storedOtp == null) {
            throw new BusinessException(ErrorCode.OTP_EXPIRED);
        }
        if (!storedOtp.equals(request.getOtp())) {
            throw new BusinessException(ErrorCode.OTP_INVALID);
        }

        // 1. Save User (Status = PENDING since they need approval)
        User user = User.builder()
                .phone(request.getPhone())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(com.fooddelivery.auth.enums.UserRole.SHIPPER)
                .status(com.fooddelivery.auth.enums.UserStatus.PENDING)
                .build();
        User savedUser = userRepository.save(user);

        // 2. Save ShipperProfile
        com.fooddelivery.auth.entity.ShipperProfile profile = com.fooddelivery.auth.entity.ShipperProfile.builder()
                .user(savedUser)
                .vehicleType(request.getVehicleType())
                .vehiclePlate(request.getVehiclePlate())
                .approvalStatus(com.fooddelivery.auth.enums.ApprovalStatus.PENDING)
                .avgRating(new java.math.BigDecimal("5.00"))
                .totalDeliveries(0)
                .build();
        shipperProfileRepository.save(profile);

        // 3. Save UserAddress
        com.fooddelivery.auth.entity.UserAddress address = com.fooddelivery.auth.entity.UserAddress.builder()
                .user(savedUser)
                .addressLine(request.getAddressLine())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .isDefault(true)
                .build();
        userAddressRepository.save(address);

        // Delete OTP from Redis
        redisTemplate.delete("otp:register:" + identifier);

        log.info("Shipper registered successfully for phone: {}", request.getPhone());
        return RegisterResponse.builder()
                .user(userConverter.toResponse(savedUser))
                .message("Shipper registration successful, pending approval")
                .build();
    }

    /**
     * Authenticate user and return JWT tokens.
     */
    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String identifier = request.getPhone(); // Frontend sends this, can be phone or email
        log.info("Login attempt for identifier: {}", identifier);

        User user = userRepository.findByPhoneOrEmail(identifier, identifier)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        validateAccountStatus(user);
        verifyPassword(request.getPassword(), user);

        resetFailedLoginCount(user);
        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user);

        storeRefreshTokenInRedis(user.getPhone(), refreshToken);

        log.info("Login successful for userId: {}", user.getId());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProvider.getAccessTokenExpirationSeconds())
                .user(userConverter.toResponse(user))
                .build();
    }

    /**
     * Refresh access token using a valid refresh token.
     */
    @Override
    @Transactional
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        log.info("Refresh token request received");

        String phone = jwtProvider.extractPhone(refreshToken);

        if (!jwtProvider.isTokenValid(refreshToken, phone)) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Refresh token is invalid or expired");
        }

        String storedToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + phone);
        if (!refreshToken.equals(storedToken)) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Refresh token has been revoked");
        }

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateAccountStatus(user);

        String newAccessToken = jwtProvider.generateAccessToken(user);
        String newRefreshToken = jwtProvider.generateRefreshToken(user);
        storeRefreshTokenInRedis(phone, newRefreshToken);

        log.info("Token refreshed for userId: {}", user.getId());

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProvider.getAccessTokenExpirationSeconds())
                .user(userConverter.toResponse(user))
                .build();
    }

    /**
     * Logout: blacklist the access token and remove the refresh token from Redis.
     */
    @Override
    public void logout(String accessToken, String phone) {
        log.info("Logout for phone: {}", phone);

        long remainingTtl = jwtProvider.getAccessTokenExpirationSeconds();
        redisTemplate.opsForValue().set(BLACKLIST_PREFIX + accessToken, "1", remainingTtl, TimeUnit.SECONDS);
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + phone);

        log.info("Logout completed for phone: {}", phone);
    }

    /**
     * Get user profile of the currently authenticated user.
     */
    @Override
    public UserResponse getMyProfile(String phone) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return userConverter.toResponse(user);
    }

    /**
     * Send OTP to email for registration verification.
     */
    @Override
    public void sendRegisterOtp(SendOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        
        validateEmailNotTaken(email);

        // Generate 6-digit OTP
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));
        
        // Save to Redis (5 minutes TTL)
        redisTemplate.opsForValue().set("otp:register:" + email, otp, 5, TimeUnit.MINUTES);
        
        // Send Email
        emailService.sendRegistrationOtpEmail(email, otp);
    }

    /**
     * Verify Register OTP.
     */
    @Override
    public void verifyRegisterOtp(com.fooddelivery.auth.dto.request.VerifyOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String otp = request.getOtp();

        String storedOtp = redisTemplate.opsForValue().get("otp:register:" + email);
        if (storedOtp == null) {
            throw new BusinessException(ErrorCode.OTP_EXPIRED);
        }
        if (!storedOtp.equals(otp)) {
            throw new BusinessException(ErrorCode.OTP_INVALID);
        }
    }

    /**
     * Send OTP to email for password reset.
     */
    @Override
    public void sendForgotPasswordOtp(SendOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        validateAccountStatus(user);

        // Generate 6-digit OTP
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));
        
        // Save to Redis (5 minutes TTL)
        redisTemplate.opsForValue().set("otp:reset:" + email, otp, 5, TimeUnit.MINUTES);
        
        // Send Email
        emailService.sendPasswordResetOtpEmail(email, otp);
    }

    /**
     * Verify Forgot Password OTP.
     */
    @Override
    public void verifyForgotPasswordOtp(com.fooddelivery.auth.dto.request.VerifyOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String otp = request.getOtp();

        String storedOtp = redisTemplate.opsForValue().get("otp:reset:" + email);
        if (storedOtp == null) {
            throw new BusinessException(ErrorCode.OTP_EXPIRED);
        }
        if (!storedOtp.equals(otp)) {
            throw new BusinessException(ErrorCode.OTP_INVALID);
        }
    }

    /**
     * Verify OTP and reset password.
     */
    @Override
    @Transactional
    public void resetPasswordWithOtp(ResetPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String otp = request.getOtp();
        String newPassword = request.getNewPassword();

        // Verify OTP from Redis
        String storedOtp = redisTemplate.opsForValue().get("otp:reset:" + email);
        if (storedOtp == null) {
            throw new BusinessException(ErrorCode.OTP_EXPIRED);
        }
        if (!storedOtp.equals(otp)) {
            throw new BusinessException(ErrorCode.OTP_INVALID);
        }

        // Get User and update password
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        validateAccountStatus(user);
        
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        resetFailedLoginCount(user);
        userRepository.save(user);

        // Delete OTP from Redis
        redisTemplate.delete("otp:reset:" + email);
        log.info("Password reset successfully for email: {}", email);
    }

    @Override
    @Transactional
    public void changePassword(String phone, com.fooddelivery.auth.dto.request.ChangePasswordRequest request) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateAccountStatus(user);

        // Verify old password
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Mật khẩu cũ không chính xác");
        }

        // Encode and set new password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Invalidate all active tokens by deleting the refresh token from Redis
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + phone);
        
        log.info("Password changed successfully for phone: {}", phone);
    }

    // ─── Private helpers ───────────────────────────────────────────────────────

    private void validatePhoneNotTaken(String phone) {
        if (userRepository.existsByPhone(phone)) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
        }
    }

    private void validateEmailNotTaken(String email) {
        if (email != null && !email.isBlank() && userRepository.existsByEmail(email.trim().toLowerCase())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    private void validateAccountStatus(User user) {
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
        if (user.getStatus() == UserStatus.PENDING) {
            throw new BusinessException(ErrorCode.ACCOUNT_PENDING);
        }
    }

    private void verifyPassword(String rawPassword, User user) {
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            incrementFailedLoginCount(user);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    private void incrementFailedLoginCount(User user) {
        short failedCount = (short) (user.getFailedLoginCount() + 1);
        user.setFailedLoginCount(failedCount);

        if (failedCount >= MAX_FAILED_LOGIN_ATTEMPTS) {
            user.setStatus(UserStatus.LOCKED);
            log.warn("Account locked due to {} failed login attempts for userId: {}", failedCount, user.getId());
        }

        userRepository.save(user);
    }

    private void resetFailedLoginCount(User user) {
        if (user.getFailedLoginCount() > 0) {
            user.setFailedLoginCount((short) 0);
        }
    }

    private void storeRefreshTokenInRedis(String phone, String refreshToken) {
        // Store refresh token in Redis with 7-day TTL (matching refresh token expiry)
        redisTemplate.opsForValue().set(REFRESH_TOKEN_PREFIX + phone, refreshToken, 7, TimeUnit.DAYS);
    }
}
