package com.goldatech.authservice.web.dto.response;

import com.goldatech.authservice.domain.model.PlanType;
import com.goldatech.authservice.domain.model.SubscriptionStatus;

public record SubscriptionAuthResponse(
        String token,
        Long subscriptionId,
        String organizationName,
        PlanType planType,
        SubscriptionStatus status,
        String contactEmail,
        String message
) {}

