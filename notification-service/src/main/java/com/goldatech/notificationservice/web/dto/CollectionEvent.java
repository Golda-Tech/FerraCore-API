package com.goldatech.notificationservice.web.dto;

import lombok.Builder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record CollectionEvent(
        String collectionRef,
        String externalRef,
        String mobileNumber,
        BigDecimal amount,
        String status,
        String message,
        String userId,
        LocalDateTime timestamp
) implements DomainEvent, Serializable {}
