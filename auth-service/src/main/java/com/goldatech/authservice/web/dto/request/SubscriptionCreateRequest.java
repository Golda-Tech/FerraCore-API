package com.goldatech.authservice.web.dto.request;

import com.goldatech.authservice.domain.model.PlanType;

public record SubscriptionCreateRequest(
        String organizationName,
        String subscriptionKey,
        String subscriptionSecret,
        PlanType planType,
        String contactEmail
) {}

