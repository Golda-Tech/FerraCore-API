package com.goldatech.collectionsservice.domain.service;

import com.goldatech.collectionsservice.domain.exception.PaymentGatewayException;
import com.goldatech.collectionsservice.web.dto.request.ExternalInitiatePaymentRequest;
import com.goldatech.collectionsservice.web.dto.response.ExternalInitiatePaymentResponse;
import com.goldatech.collectionsservice.web.dto.response.ExternalPaymentDetailsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalPaymentGatewayService {

    private final RestTemplate restTemplate;

    @Value("${external.payment.api.base-url}")
    private String externalApiBaseUrl;

    @Value("${external.payment.api.username}")
    private String externalApiUsername;

    @Value("${external.payment.api.password}")
    private String externalApiPassword;

    /**
     * Initiates a payment collection with the external payment gateway.
     */
    public ExternalInitiatePaymentResponse initiatePayment(ExternalInitiatePaymentRequest request) {
        log.info("Calling external payment API to initiate payment for reference: {}", request.reference());

        String url = externalApiBaseUrl + "/api/pgs/payment/v1/makePayment";
        HttpHeaders headers = createAuthHeaders();
        HttpEntity<ExternalInitiatePaymentRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<ExternalInitiatePaymentResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    ExternalInitiatePaymentResponse.class
            );

            if (response.getBody() == null) {
                throw new PaymentGatewayException("Null response received from external payment API for reference " + request.reference());
            }

            log.info("External payment API initiatePayment response for {}: {}", request.reference(), response.getBody());
            return response.getBody();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("External payment API initiatePayment failed for reference {}: Status {}, Body: {}",
                    request.reference(), e.getStatusCode(), e.getResponseBodyAsString());

            boolean isFatal = e.getStatusCode().is4xxClientError();
            throw new PaymentGatewayException("External payment API failed: " + e.getResponseBodyAsString(), e, isFatal);

        } catch (Exception e) {
            log.error("Error calling external payment API for reference {}: {}", request.reference(), e.getMessage(), e);
            throw new PaymentGatewayException("An unexpected error occurred during external payment initiation for reference " + request.reference(), e);
        }
    }

    /**
     * Retrieves transaction details from the external payment gateway using its reference.
     */
    public ExternalPaymentDetailsResponse getPaymentDetails(String transactionReference) {
        log.info("Calling external payment API to get transaction details for reference: {}", transactionReference);

        String url = externalApiBaseUrl + "/api/pgs/payment/v1/getPaymentDetails/" + transactionReference;
        HttpHeaders headers = createAuthHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<ExternalPaymentDetailsResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    ExternalPaymentDetailsResponse.class
            );

            if (response.getBody() == null) {
                throw new PaymentGatewayException("Null response received from external payment API for reference " + transactionReference);
            }

            log.info("External payment details for {}: {}", transactionReference, response.getBody());
            return response.getBody();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("External payment API getPaymentDetails failed for reference {}: Status {}, Body: {}",
                    transactionReference, e.getStatusCode(), e.getResponseBodyAsString());

            boolean isFatal = e.getStatusCode().is4xxClientError();
            throw new PaymentGatewayException("External payment API failed: " + e.getResponseBodyAsString(), e, isFatal);

        } catch (Exception e) {
            log.error("Error getting external payment details for {}: {}", transactionReference, e.getMessage(), e);
            throw new PaymentGatewayException("An unexpected error occurred during external payment details retrieval for reference " + transactionReference, e);
        }
    }

    /**
     * Creates HTTP headers with Basic Authentication.
     */
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String auth = externalApiUsername + ":" + externalApiPassword;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);

        return headers;
    }
}