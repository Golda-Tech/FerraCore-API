package com.goldatech.collectionsservice.domain.service;

import com.goldatech.collectionsservice.domain.exception.PaymentGatewayException;
import com.goldatech.collectionsservice.web.dto.request.ExternalInitiatePaymentRequest;
import com.goldatech.collectionsservice.web.dto.request.ExternalPaymentApiRequest;
import com.goldatech.collectionsservice.web.dto.response.ExternalInitiatePaymentResponse;
import com.goldatech.collectionsservice.web.dto.response.ExternalPaymentApiResponse;
import com.goldatech.collectionsservice.web.dto.response.ExternalPaymentDetailsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalPaymentGatewayService {

    private final WebClient.Builder webClientBuilder;

    @Value("${external.payment.api.base-url}")
    private String externalApiBaseUrl;

    @Value("${external.payment.api.username}")
    private String externalApiUsername;

    @Value("${external.payment.api.password}")
    private String externalApiPassword;

    /**
     * Initiates a payment collection with the external payment gateway.
     *
     * @param request The ExternalInitiatePaymentRequest containing payment details.
     * @return A Mono of ExternalInitiatePaymentResponse with the gateway's response.
     * @throws PaymentGatewayException if the external API call fails or returns an error.
     */
    public Mono<ExternalInitiatePaymentResponse> initiatePayment(ExternalInitiatePaymentRequest request) {
        log.info("Calling external payment API to initiate payment for reference: {}", request.reference());

        // Encode Basic Auth credentials
        String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                (externalApiUsername + ":" + externalApiPassword).getBytes()
        );

        WebClient webClient = webClientBuilder.baseUrl(externalApiBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, authHeader)
                .build();

        return webClient.post()
                .uri("/api/pgs/payment/v1/makePayment")
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("External payment API initiatePayment failed for reference {}: Status {}, Body: {}",
                                            request.reference(), clientResponse.statusCode(), errorBody);
                                    return Mono.error(new PaymentGatewayException("External payment API failed: " + errorBody));
                                })
                )
                .bodyToMono(ExternalInitiatePaymentResponse.class)
                .timeout(Duration.ofSeconds(15)) // Increased timeout for external calls
                .doOnSuccess(response -> log.info("External payment API initiatePayment response for {}: {}", request.reference(), response))
                .doOnError(e -> log.error("Error calling external payment API for reference {}: {}", request.reference(), e.getMessage(), e))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)) // Retry up to 3 times with a 2-second backoff
                        .filter(e -> e instanceof PaymentGatewayException && !((PaymentGatewayException) e).isFatal()) // Only retry non-fatal exceptions
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new PaymentGatewayException("External payment API initiatePayment failed after multiple retries for reference " + request.reference(), retrySignal.failure()))
                )
                .onErrorMap(RuntimeException.class, e -> new PaymentGatewayException("An unexpected error occurred during external payment initiation for reference " + request.reference(), e));
    }

    /**
     * Retrieves transaction details from the external payment gateway using its reference.
     *
     * @param transactionReference The transaction reference ID provided by the external gateway.
     * @return A Mono of ExternalPaymentDetailsResponse containing detailed transaction status.
     * @throws PaymentGatewayException if the external API call fails or returns an error.
     */
    public Mono<ExternalPaymentDetailsResponse> getPaymentDetails(String transactionReference) {
        log.info("Calling external payment API to get transaction details for reference: {}", transactionReference);

        // Encode Basic Auth credentials
        String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                (externalApiUsername + ":" + externalApiPassword).getBytes()
        );

        WebClient webClient = webClientBuilder.baseUrl(externalApiBaseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, authHeader)
                .build();

        return webClient.get()
                .uri("/api/pgs/payment/v1/getPaymentDetails/{TransReference}", transactionReference)
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("External payment API getPaymentDetails failed for reference {}: Status {}, Body: {}",
                                            transactionReference, clientResponse.statusCode(), errorBody);
                                    return Mono.error(new PaymentGatewayException("External payment API failed: " + errorBody));
                                })
                )
                .bodyToMono(ExternalPaymentDetailsResponse.class)
                .timeout(Duration.ofSeconds(10))
                .doOnSuccess(response -> log.info("External payment details for {}: {}", transactionReference, response))
                .doOnError(e -> log.error("Error getting external payment details for {}: {}", transactionReference, e.getMessage(), e))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(e -> e instanceof PaymentGatewayException && !((PaymentGatewayException) e).isFatal())
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) ->
                                new PaymentGatewayException("External payment API getPaymentDetails failed after multiple retries for reference " + transactionReference, retrySignal.failure()))
                )
                .onErrorMap(RuntimeException.class, e -> new PaymentGatewayException("An unexpected error occurred during external payment details retrieval for reference " + transactionReference, e));
    }
}