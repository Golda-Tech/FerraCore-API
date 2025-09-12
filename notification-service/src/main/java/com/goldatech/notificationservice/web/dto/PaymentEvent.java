package com.goldatech.notificationservice.web.dto;

import lombok.Builder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record PaymentEvent(
        String transactionRef,
        String externalRef,
        String collectionRef,
        String mobileNumber,
        BigDecimal amount,
        String currency,
        String status,
        String message,
        String userId,
        LocalDateTime timestamp
)  {}