package com.goldatech.notificationservice.web.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record RegistrationEvent(

        String organizationName,
        String email,
        String eventAction,
        String message,
        String planType,
        LocalDateTime timestamp
) {
}
