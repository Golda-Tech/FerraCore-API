package com.goldatech.authservice.web.dto.response;

import java.util.List;

public record SubscriptionListResponse(
        List<SubscriptionResponse> subscriptions
) {}
