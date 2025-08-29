package com.goldatech.collectionsservice.web.dto.request;

/**
 * Data Transfer Object (DTO) for receiving callbacks from the external payment API.
 * This should mirror the structure of the callback payload from your third-party provider.
 *
 * @param transactionId The transaction ID that your service provided to the external API (clientTransactionId).
 * @param externalTransactionId The transaction ID from the external payment gateway.
 * @param status The updated status of the payment (e.g., "SUCCESS", "FAILED").
 * @param reason An optional reason for the status update (e.g., "Insufficient funds").
 */
public record PaymentCallbackRequest(
        String transactionId,
        String externalTransactionId,
        String status,
        String reason
) {}