package com.fooddelivery.auth.service.impl;

import com.fooddelivery.auth.dto.request.AddAddressRequest;
import com.fooddelivery.auth.dto.response.UserAddressDto;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.entity.UserAddress;
import com.fooddelivery.auth.exception.BusinessException;
import com.fooddelivery.auth.enums.ErrorCode;
import com.fooddelivery.auth.repository.UserAddressRepository;
import com.fooddelivery.auth.repository.UserRepository;
import com.fooddelivery.auth.service.UserAddressService;
import com.fooddelivery.auth.service.external.VietmapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl implements UserAddressService {

    private final UserAddressRepository userAddressRepository;
    private final UserRepository userRepository;
    private final VietmapService vietmapService;

    @Override
    public List<UserAddressDto> getUserAddresses(String phone) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return userAddressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserAddressDto addAddress(String phone, AddAddressRequest request) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (request.getIsDefault() != null && request.getIsDefault()) {
            userAddressRepository.updateIsDefaultToFalse(user.getId());
        }

        // Call VietMap API to get coordinates
        VietmapService.Coordinates coordinates = vietmapService.geocodeAddress(request.getAddressLine());
        
        if (coordinates == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Địa chỉ không hợp lệ hoặc không tìm thấy toạ độ. Vui lòng nhập rõ hơn.");
        }

        UserAddress address = UserAddress.builder()
                .user(user)
                .addressLine(request.getAddressLine())
                .latitude(coordinates.getLatitude())
                .longitude(coordinates.getLongitude())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .build();

        address = userAddressRepository.save(address);
        log.info("Added new address for user {}: {}", user.getId(), address.getId());

        return mapToDto(address);
    }

    @Override
    @Transactional
    public void deleteAddress(String phone, Long addressId) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserAddress address = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "Address not found"));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "You don't have permission to delete this address");
        }

        userAddressRepository.delete(address);
        log.info("Deleted address {} for user {}", addressId, user.getId());
    }

    @Override
    @Transactional
    public void setDefaultAddress(String phone, Long addressId) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserAddress address = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "Address not found"));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "You don't have permission to update this address");
        }

        userAddressRepository.updateIsDefaultToFalse(user.getId());
        address.setIsDefault(true);
        userAddressRepository.save(address);
    }

    private UserAddressDto mapToDto(UserAddress address) {
        return UserAddressDto.builder()
                .id(address.getId())
                .addressLine(address.getAddressLine())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .isDefault(address.getIsDefault())
                .createdAt(address.getCreatedAt())
                .build();
    }
}
