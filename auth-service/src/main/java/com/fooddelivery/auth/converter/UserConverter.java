package com.fooddelivery.auth.converter;

import com.fooddelivery.auth.dto.request.RegisterRequest;
import com.fooddelivery.auth.dto.response.UserResponse;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.enums.UserRole;
import com.fooddelivery.auth.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserConverter {

    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * Convert RegisterRequest → User Entity.
     * Business logic: encode password, set default role/status, normalize phone.
     */
    public User toEntity(RegisterRequest request) {
        UserRole role = (request.getRole() != null) ? request.getRole() : UserRole.CUSTOMER;

        return User.builder()
                .phone(request.getPhone().trim())
                .email(request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null)
                .fullName(request.getFullName() != null ? request.getFullName().trim() : null)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .status(UserStatus.ACTIVE)
                .areaId(request.getAreaId())
                .isAreaVerified(false)
                .failedLoginCount((short) 0)
                .build();
    }

    /**
     * Convert User Entity → UserResponse DTO.
     * Strips sensitive fields (passwordHash, tokens, etc.).
     */
    public UserResponse toResponse(User user) {
        return modelMapper.map(user, UserResponse.class);
    }
}
