package com.fooddelivery.auth.controller;

import com.fooddelivery.auth.common.ApiResponse;
import com.fooddelivery.auth.dto.request.AddAddressRequest;
import com.fooddelivery.auth.dto.response.UserAddressDto;
import com.fooddelivery.auth.service.UserAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/auth/addresses")
@RequiredArgsConstructor
public class UserAddressController {

    private final UserAddressService userAddressService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserAddressDto>>> getAddresses(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<UserAddressDto> addresses = userAddressService.getUserAddresses(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Addresses retrieved successfully", addresses));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserAddressDto>> addAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AddAddressRequest request
    ) {
        UserAddressDto address = userAddressService.addAddress(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Address added successfully", address));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        userAddressService.deleteAddress(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success("Address deleted successfully", null));
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<ApiResponse<Void>> setDefaultAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        userAddressService.setDefaultAddress(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success("Address set as default successfully", null));
    }
}
