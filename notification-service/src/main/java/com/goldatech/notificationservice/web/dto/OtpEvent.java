package com.goldatech.notificationservice.web.dto;

public record OtpEvent(
        String mobileNumber,
        String email,
        String otpCode,
        String userId,
        String type,
        String message,
        String channel,
        String subject
) {
}
