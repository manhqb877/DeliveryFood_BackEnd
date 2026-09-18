package com.fooddelivery.auth.service;

import com.fooddelivery.auth.dto.request.LoginRequest;
import com.fooddelivery.auth.dto.request.RefreshTokenRequest;
import com.fooddelivery.auth.dto.request.RegisterRequest;
import com.fooddelivery.auth.dto.response.LoginResponse;
import com.fooddelivery.auth.dto.response.RegisterResponse;
import com.fooddelivery.auth.dto.response.UserResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    LoginResponse refreshToken(RefreshTokenRequest request);

    void logout(String accessToken, String phone);

    UserResponse getMyProfile(String phone);
}
