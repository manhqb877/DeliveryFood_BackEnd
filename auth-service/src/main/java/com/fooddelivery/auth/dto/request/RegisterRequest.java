package com.fooddelivery.auth.dto.request;

import com.fooddelivery.auth.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Phone number must not be blank")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Phone number must be a valid 10-digit phone number")
    private String phone;

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email format is invalid")
    private String email;

    @NotBlank(message = "OTP must not be blank")
    private String otp;

    @NotBlank(message = "Full name must not be blank")
    private String fullName;

    @NotBlank(message = "Password must not be blank")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private UserRole role; // CUSTOMER, SHOP_MANAGER, SHIPPER

    private Long areaId; // Area ID if associated with a specific area
}
