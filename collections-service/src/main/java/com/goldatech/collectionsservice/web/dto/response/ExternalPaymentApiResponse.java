package com.goldatech.collectionsservice.web.dto.response;

public record ExternalPaymentApiResponse(
        String externalTransactionId,
        String status,
        String message
) {}

