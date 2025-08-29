package com.goldatech.collectionsservice.web.controller;

import com.goldatech.collectionsservice.domain.service.CollectionService;
import com.goldatech.collectionsservice.web.dto.request.PaymentCallbackRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/collections/callback")
@RequiredArgsConstructor
@Slf4j // For logging
public class PaymentCallbackController {

    private final CollectionService collectionService;

    /**
     * Receives and processes payment status updates from the external payment gateway.
     *
     * @param callbackRequest The PaymentCallbackRequest containing the update from the gateway.
     * @return A Mono of ResponseEntity indicating the status of the callback processing.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public Mono<ResponseEntity<String>> handlePaymentCallback(@RequestBody PaymentCallbackRequest callbackRequest) {
        log.info("Received payment callback for transaction ID: {}", callbackRequest.transactionId());
        // Delegate to the CollectionService to update the collection status
        return collectionService.handlePaymentCallback(callbackRequest)
                .thenReturn(ResponseEntity.ok("Callback processed successfully"))
                .onErrorResume(e -> {
                    log.error("Error processing payment callback for transaction ID {}: {}",
                            callbackRequest.transactionId(), e.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Error processing callback: " + e.getMessage()));
                });
    }
}
