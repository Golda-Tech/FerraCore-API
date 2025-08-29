package com.goldatech.collectionsservice.web.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Data Transfer Object (DTO) for receiving responses from the external payment API
 * when retrieving payment details via the 'getPaymentDetails' endpoint.
 * This mirrors the structure returned by your third-party payment provider.
 *
 * @param referenceId The reference ID used for the transaction.
 * @param clientId The client ID from the external system.
 * @param userId The user ID associated with the payment.
 * @param amount The amount of the payment.
 * @param fees Any fees associated with the payment.
 * @param currency The currency of the payment.
 * @param createdAt The timestamp when the payment was created.
 * @param channel The payment channel.
 * @param status The current status of the payment.
 * @param statusMessage A descriptive status message.
 * @param providerReference The provider's internal reference for the transaction.
 * @param provider The payment provider.
 * @param providerInitiated Boolean indicating if the provider initiated the transaction.
 * @param platformSettled Boolean indicating if the platform has settled the transaction.
 * @param providerResponse The raw response from the provider.
 * @param callbackURL The callback URL used for this transaction.
 * @param clientLogo The URL to the client's logo.
 * @param clientName The name of the client.
 * @param metadata Any additional metadata.
 */
public record ExternalPaymentDetailsResponse(
        String referenceId,
        String clientId,
        String userId,
        BigDecimal amount,
        BigDecimal fees,
        String currency,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSSZ")
        LocalDateTime createdAt,
        String channel,
        String status,
        String statusMessage,
        String providerReference,
        String provider,
        boolean providerInitiated,
        boolean platformSettled,
        String providerResponse,
        String callbackURL,
        String clientLogo,
        String clientName,
        Map<String, Object> metadata
) {}
