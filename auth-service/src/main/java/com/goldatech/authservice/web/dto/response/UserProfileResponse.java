package com.goldatech.authservice.web.dto.response;

import com.goldatech.authservice.domain.model.PlanType;
import com.goldatech.authservice.domain.model.Role;
import com.goldatech.authservice.domain.model.SubscriptionStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record UserProfileResponse(
        String id,
        String firstName,
        String lastName,
        String email,
        Boolean isFirstTimeUser,
        String phone,
        Role role,
        OrganizationDetails organization,
        SubscriptionDetails subscription,
        ApiCredentials apiCredentials
) {
    @Builder
    public record OrganizationDetails(
            String name,
            String businessType,
            String address,
            String registrationNumber,
            String taxId,
            String website
    ) {}

    @Builder
    public record SubscriptionDetails(
            String plan,
            SubscriptionStatus status,
            String billingCycle,
            LocalDateTime nextBilling,
            Double amount,
            String currency
    ) {}

    @Builder
    public record ApiCredentials(
            String subscriptionKey,
            String subscriptionSecret
    ) {}
}
