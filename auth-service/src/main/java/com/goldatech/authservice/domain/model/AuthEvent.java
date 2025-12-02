package com.goldatech.authservice.domain.model;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AuthEvent(
        String username,
        String email,
        String eventAction,
        String message,
        String userId,
        LocalDateTime timestamp
)  {}

