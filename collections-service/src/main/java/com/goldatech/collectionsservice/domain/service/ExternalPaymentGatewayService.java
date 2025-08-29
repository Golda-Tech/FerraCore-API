package com.goldatech.collectionsservice.domain.service;

import com.goldatech.collectionsservice.web.dto.request.ExternalPaymentApiRequest;
import com.goldatech.collectionsservice.web.dto.response.ExternalPaymentApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalPaymentGatewayService {

    private final WebClient.Builder webClientBuilder;

    @Value("${external.payment.api.base-url}")
    private String externalApiBaseUrl;

    @Value("${external.payment.api.key}")
    private String externalApiKey; // For authentication with external API

    /**
     * Initiates a payment collection with the external payment gateway.
     *
     * @param request The ExternalPaymentApiRequest containing payment details.
     * @return A Mono of ExternalPaymentApiResponse with the gateway's response.
     */
    public Mono<ExternalPaymentApiResponse> initiatePayment(ExternalPaymentApiRequest request) {
        log.info("Calling external payment API to initiate payment for clientTransactionId: {}", request.clientTransactionId());

        WebClient webClient = webClientBuilder.baseUrl(externalApiBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-Api-Key", externalApiKey) // Example for API key authentication
                .build();

        return webClient.post()
                .uri("/payments") // Example endpoint for initiating payments
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ExternalPaymentApiResponse.class)
                .timeout(Duration.ofSeconds(10)) // Set a timeout for the external call
                .doOnSuccess(response -> log.info("External payment API response for {}: {}", request.clientTransactionId(), response))
                .doOnError(e -> log.error("Error calling external payment API for {}: {}", request.clientTransactionId(), e.getMessage()));
    }

    /**
     * Retrieves transaction details from the external payment gateway using its reference.
     *
     * @param externalTransactionId The transaction ID provided by the external gateway.
     * @return A Mono of ExternalPaymentApiResponse containing detailed transaction status.
     */
    public Mono<ExternalPaymentApiResponse> getTransactionDetails(String externalTransactionId) {
        log.info("Calling external payment API to get transaction details for externalTransactionId: {}", externalTransactionId);

        WebClient webClient = webClientBuilder.baseUrl(externalApiBaseUrl)
                .defaultHeader("X-Api-Key", externalApiKey) // Example for API key authentication
                .build();

        return webClient.get()
                .uri("/payments/{id}", externalTransactionId) // Example endpoint for getting details
                .retrieve()
                .bodyToMono(ExternalPaymentApiResponse.class)
                .timeout(Duration.ofSeconds(5))
                .doOnSuccess(response -> log.info("External payment details for {}: {}", externalTransactionId, response))
                .doOnError(e -> log.error("Error getting external payment details for {}: {}", externalTransactionId, e.getMessage()));
    }
}