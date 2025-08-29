package com.goldatech.collectionsservice.web.dto.response;

import com.goldatech.collectionsservice.domain.model.CollectionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CollectionResponse(
        Long id,
        String collectionRef,
        String externalRef,
        BigDecimal amount,
        String currency,
        String customerId,
        CollectionStatus status,
        LocalDateTime initiatedAt,
        LocalDateTime updatedAt,
        String message
) {}
