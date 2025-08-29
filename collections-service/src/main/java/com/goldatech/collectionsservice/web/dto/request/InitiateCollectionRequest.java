package com.goldatech.collectionsservice.web.dto.request;

import java.math.BigDecimal;

public record InitiateCollectionRequest(
        BigDecimal amount,
        String currency,
        String customerId,
        String description
) {}
