package com.fooddelivery.auth.service;

import com.fooddelivery.auth.dto.response.UserResponse;
import com.fooddelivery.auth.enums.UserStatus;

import java.util.List;

public interface AdminUserService {
    List<UserResponse> getAllUsers();
    void updateUserStatus(Long id, UserStatus status);
    void resetUserPassword(Long id, String newPassword);
}
