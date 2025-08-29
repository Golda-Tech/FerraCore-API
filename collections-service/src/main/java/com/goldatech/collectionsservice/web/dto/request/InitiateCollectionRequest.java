package com.goldatech.collectionsservice.web.dto.request;

import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) for client requests to initiate a collection.
 *
 * @param amount The amount to collect.
 * @param currency The currency of the collection.
 * @param customerId The ID of the customer associated with this collection.
 * @param description A brief description of the collection purpose.
 * @param paymentChannel The channel through which the payment is expected (e.g., "USSD", "CARD").
 * @param provider The payment provider (e.g., "MTN", "VISA").
 * @param merchantName The name of the merchant.
 */
public record InitiateCollectionRequest(
        BigDecimal amount,
        String currency,
        String customerId,
        String description,
        String paymentChannel,
        String provider,
        String merchantName
) {}