package com.goldatech.authservice.web.dto.response;

public record RegistrationResponse(
        String token,
        String email,
        boolean passwordResetRequired,
        SubscriptionResponse subscription,
        String message
) {}
