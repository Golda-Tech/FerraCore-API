package com.goldatech.authservice.web.dto.request;

import com.goldatech.authservice.domain.model.PlanType;
import com.goldatech.authservice.domain.model.UserRoles;

public record SubscriptionCreateRequest(
        String organizationName,
        PlanType planType,
        UserRoles userType,
        String contactEmail,
        String contactNumber,
        String address
) {}

