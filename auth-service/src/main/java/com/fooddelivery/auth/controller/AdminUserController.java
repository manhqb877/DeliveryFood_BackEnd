package com.fooddelivery.auth.controller;

import com.fooddelivery.auth.dto.request.AdminResetPasswordRequest;
import com.fooddelivery.auth.dto.response.ApiResponse;
import com.fooddelivery.auth.dto.response.UserResponse;
import com.fooddelivery.auth.enums.UserRole;
import com.fooddelivery.auth.enums.UserStatus;
import com.fooddelivery.auth.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<ApiResponse> getAllUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status
    ) {
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Fetched all users successfully")
                .data(adminUserService.getAllUsers(keyword, role, status))
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getUserDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Fetched user detail successfully")
                .data(adminUserService.getUserDetail(id))
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateUserDetail(
            @PathVariable Long id,
            @Valid @RequestBody com.fooddelivery.auth.dto.request.AdminUpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Updated user detail successfully")
                .data(adminUserService.updateUserDetail(id, request))
                .build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse> updateUserStatus(
            @PathVariable Long id,
            @RequestParam UserStatus status) {
        adminUserService.updateUserStatus(id, status);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Updated user status successfully")
                .build());
    }

    @PutMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse> resetUserPassword(
            @PathVariable Long id,
            @Valid @RequestBody AdminResetPasswordRequest request) {
        adminUserService.resetUserPassword(id, request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Reset user password successfully")
                .build());
    }
}
