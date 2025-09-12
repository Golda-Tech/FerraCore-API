package com.goldatech.notificationservice.web.dto;

import lombok.Builder;

import java.io.Serializable;
import java.time.LocalDateTime;

@Builder
public record AuthEvent(
        String username,
        String email,
        String eventAction,
        String message,
        String userId,
        LocalDateTime timestamp
) implements DomainEvent, Serializable {}
