package com.goldatech.authservice.web.dto.response;

import com.goldatech.authservice.domain.model.PlanType;
import com.goldatech.authservice.domain.model.SubscriptionStatus;

public record Organization(
        String name,
        String address,
        PlanType planType,
        SubscriptionStatus subscriptionStatus,
        String phoneNumber,
        String email
) {
}
