package com.fooddelivery.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddAddressRequest {
    @NotBlank(message = "Address line cannot be blank")
    private String addressLine;
    
    private Boolean isDefault = false;
}
