package com.goldatech.authservice.web.dto.response;

import com.goldatech.authservice.domain.model.PlanType;
import com.goldatech.authservice.domain.model.SubscriptionStatus;

public record SubscriptionAuthResponse(
        String access_token,
        Integer expires_in,
        Long subscriptionId,
        Organization organization_info,
        String message
) {}

