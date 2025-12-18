package com.goldatech.authservice.web.dto.request;

import com.goldatech.authservice.domain.model.PlanType;
import com.goldatech.authservice.domain.model.Role;

public record SubscriptionCreateRequest(
        String organizationName,
        PlanType planType,
        Role userType,
        String contactEmail,
        String contactNumber,
        String address
) {}

