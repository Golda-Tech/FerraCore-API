package com.goldatech.authservice.domain.mapper;

import com.goldatech.authservice.domain.model.Subscription;
import com.goldatech.authservice.web.dto.request.SubscriptionCreateRequest;
import com.goldatech.authservice.web.dto.request.SubscriptionUpdateRequest;
import com.goldatech.authservice.web.dto.response.SubscriptionResponse;

public class SubscriptionMapper {

    public static SubscriptionResponse toResponse(Subscription entity) {
        return new SubscriptionResponse(
                entity.getId(),
                entity.getOrganizationName(),
                entity.getSubscriptionKey(),
                entity.getSubscriptionSecret(),
                entity.getPlanType(),
                entity.getStatus(),
                entity.getContactEmail(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static Subscription toEntity(SubscriptionCreateRequest request) {
        return Subscription.builder()
                .organizationName(request.organizationName())
                .subscriptionKey(request.subscriptionKey())
                .subscriptionSecret(request.subscriptionSecret())
                .planType(request.planType())
                .contactEmail(request.contactEmail())
                .status("ACTIVE")
                .build();
    }

    public static void updateEntity(Subscription entity, SubscriptionUpdateRequest request) {
        entity.setOrganizationName(request.organizationName());
        entity.setSubscriptionSecret(request.subscriptionSecret());
        entity.setPlanType(request.planType());
        entity.setStatus(request.status());
        entity.setContactEmail(request.contactEmail());
    }
}
