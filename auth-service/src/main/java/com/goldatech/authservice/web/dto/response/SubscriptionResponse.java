package com.goldatech.authservice.web.dto.response;

import com.goldatech.authservice.domain.model.PlanType;
import com.goldatech.authservice.domain.model.SubscriptionStatus;

import java.time.LocalDateTime;

public record SubscriptionResponse(
        Long id,
        String organizationName,
        String subscriptionKey,
        String subscriptionSecret,
        String organizationId,
        PlanType planType,
        SubscriptionStatus status,
        String contactEmail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

