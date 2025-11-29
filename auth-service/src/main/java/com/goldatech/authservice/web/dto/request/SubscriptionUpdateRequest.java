package com.goldatech.authservice.web.dto.request;

import com.goldatech.authservice.domain.model.PlanType;
import com.goldatech.authservice.domain.model.SubscriptionStatus;

public record SubscriptionUpdateRequest(
        String organizationName,
        PlanType planType,
        SubscriptionStatus status,
        String contactEmail
) {}

