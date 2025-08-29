package com.goldatech.collectionsservice.web.dto.request;

import java.math.BigDecimal;

public record ExternalPaymentApiRequest(
        BigDecimal amount,
        String currency,
        String customerId,
        String clientTransactionId,
        String description,
        String callbackUrl
) {}
