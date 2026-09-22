package com.fooddelivery.auth.dto.request;

import com.fooddelivery.auth.enums.VehicleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterShipperRequest {

    @NotBlank(message = "Phone number must not be blank")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Phone number must be a valid 10-digit phone number")
    private String phone;

    private String email; // Made email optional for shippers

    @NotBlank(message = "OTP must not be blank")
    private String otp;

    @NotBlank(message = "Full name must not be blank")
    private String fullName;

    @NotBlank(message = "Password must not be blank")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotNull(message = "Vehicle type must not be null")
    private VehicleType vehicleType;

    @NotBlank(message = "Vehicle plate must not be blank")
    private String vehiclePlate;

    @NotBlank(message = "Address line must not be blank")
    private String addressLine;

    private BigDecimal latitude;
    private BigDecimal longitude;
}
