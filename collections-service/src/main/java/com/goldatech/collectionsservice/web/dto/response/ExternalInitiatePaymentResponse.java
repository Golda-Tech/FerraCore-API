package com.goldatech.collectionsservice.web.dto.response;

/**
 * Data Transfer Object (DTO) for receiving responses from the external payment API
 * when initiating a payment via the 'makePayment' endpoint.
 * This mirrors the structure returned by your third-party payment provider.
 *
 * @param reference The reference ID from our request.
 * @param clientId The client ID from the external system.
 * @param status The initial status of the transaction (e.g., "ONGOING").
 * @param paymentChannel The payment channel used.
 * @param providerResponse A descriptive response from the provider.
 * @param providerExtraInfo Extra information from the provider (e.g., "MTN").
 * @param externalPaymentReference The unique transaction reference from the external payment gateway.
 */
public record ExternalInitiatePaymentResponse(
        String reference,
        String clientId,
        String status,
        String paymentChannel,
        String providerResponse,
        String providerExtraInfo,
        String externalPaymentReference
) {}

