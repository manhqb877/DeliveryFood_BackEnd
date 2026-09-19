package com.fooddelivery.auth.service;

import com.fooddelivery.auth.dto.request.AddAddressRequest;
import com.fooddelivery.auth.dto.response.UserAddressDto;

import java.util.List;

public interface UserAddressService {
    List<UserAddressDto> getUserAddresses(String phone);
    UserAddressDto addAddress(String phone, AddAddressRequest request);
    void deleteAddress(String phone, Long addressId);
    void setDefaultAddress(String phone, Long addressId);
}
