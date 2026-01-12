package com.goldatech.authservice.web.dto.response;

import com.goldatech.authservice.domain.model.UserRoles;
import com.goldatech.authservice.domain.model.SubscriptionStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record UserProfileResponse(
        String id,
        String firstName,
        String lastName,
        String email,
        Boolean isFirstTimeUser,
        String phone,
        UserRoles userRoles,
        OrganizationDetails organization,
        SubscriptionDetails subscription,
        ApiCredentials apiCredentials,
        Summary summary
) {
    @Builder
    public record OrganizationDetails(
            String name,
            String partnerId,
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
            String callbackUrl,
            String whitelistedNumber1,
            String whitelistedNumber2,
            String whitelistedNumber3,
            String whitelistedNumber4,
            Double amount,
            String currency
    ) {}

    @Builder
    public record ApiCredentials(
            String subscriptionKey,
            String subscriptionSecret
    ) {}

    @Builder
    public record Summary(
            String partnerId,
            String partnerName,
            Integer totalCountTransactions,
            Long successfulTransactionsCount,
            Long failedTransactionsCount,
            BigDecimal totalSuccessfulAmountTransactions
    ) {}
}
