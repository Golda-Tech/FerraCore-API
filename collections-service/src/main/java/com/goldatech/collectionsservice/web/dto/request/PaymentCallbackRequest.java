package com.goldatech.collectionsservice.web.dto.request;

public record PaymentCallbackRequest(
        String transactionId,
        String externalTransactionId,
        String status,
        String reason
) {}