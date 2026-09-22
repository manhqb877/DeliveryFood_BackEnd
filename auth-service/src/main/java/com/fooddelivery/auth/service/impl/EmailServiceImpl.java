package com.fooddelivery.auth.service.impl;

import com.fooddelivery.auth.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Override
    public void sendRegistrationOtpEmail(String toEmail, String otp) {
        log.info("Sending Registration OTP to email: {}", toEmail);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(toEmail);
            message.setSubject("DeliveryFood - Registration OTP");
            message.setText("Hello,\n\nYour OTP for registration is: " + otp + "\n\nThis OTP will expire in 5 minutes.\nIf you did not request this, please ignore this email.\n\nThanks,\nDeliveryFood Team");

            javaMailSender.send(message);
            log.info("Registration OTP sent successfully to email: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}", toEmail, e);
            throw new RuntimeException("Failed to send email");
        }
    }

    @Override
    public void sendPasswordResetOtpEmail(String toEmail, String otp) {
        log.info("Sending Password Reset OTP to email: {}", toEmail);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(toEmail);
            message.setSubject("DeliveryFood - Password Reset OTP");
            message.setText("Hello,\n\nYour OTP for password reset is: " + otp + "\n\nThis OTP will expire in 5 minutes.\nIf you did not request this, please ignore this email.\n\nThanks,\nDeliveryFood Team");

            javaMailSender.send(message);
            log.info("Password Reset OTP sent successfully to email: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}", toEmail, e);
            throw new RuntimeException("Failed to send email");
        }
    }
}
