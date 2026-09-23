package com.fooddelivery.auth.service;

import com.fooddelivery.auth.dto.request.AdminUpdateUserRequest;
import com.fooddelivery.auth.dto.response.UserDetailResponse;
import com.fooddelivery.auth.dto.response.UserResponse;
import com.fooddelivery.auth.enums.UserRole;
import com.fooddelivery.auth.enums.UserStatus;

import java.util.List;

public interface AdminUserService {
    List<UserResponse> getAllUsers(String keyword, UserRole role, UserStatus status);
    UserDetailResponse getUserDetail(Long id);
    UserDetailResponse updateUserDetail(Long id, AdminUpdateUserRequest request);
    void updateUserStatus(Long id, UserStatus status);
    void resetUserPassword(Long id, String newPassword);
}
