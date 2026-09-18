package com.fooddelivery.auth.service;

public interface EmailService {
    void sendOtpEmail(String toEmail, String otp);
}
