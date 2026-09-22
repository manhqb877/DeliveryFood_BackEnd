package com.fooddelivery.auth.service;

public interface EmailService {
    void sendRegistrationOtpEmail(String toEmail, String otp);
    void sendPasswordResetOtpEmail(String toEmail, String otp);
}
