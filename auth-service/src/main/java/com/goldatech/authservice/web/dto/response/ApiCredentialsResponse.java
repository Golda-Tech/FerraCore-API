package com.goldatech.authservice.web.dto.response;

import lombok.Builder;

@Builder
public record ApiCredentialsResponse(
        String subscriptionKey,
        String subscriptionSecret
) {}
