package com.goldatech.authservice.web.dto.request;

public record SubscriptionLoginRequest(
        String subscriptionKey,
        String subscriptionSecret
) {}

