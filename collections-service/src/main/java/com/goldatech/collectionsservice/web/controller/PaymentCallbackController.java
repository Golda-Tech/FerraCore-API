package com.goldatech.collectionsservice.web.controller;

import com.goldatech.collectionsservice.domain.service.CollectionService;
import com.goldatech.collectionsservice.web.dto.request.PaymentCallbackRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/collections/callback")
@RequiredArgsConstructor
@Slf4j
public class PaymentCallbackController {

    private final CollectionService collectionService;

    /**
     * Receives and processes payment status updates from the external payment gateway.
     */
    @PostMapping
    public ResponseEntity<String> handlePaymentCallback(@RequestBody PaymentCallbackRequest callbackRequest) {
        log.info("Received payment callback for transaction ID: {}", callbackRequest.transactionId());

        try {
            collectionService.handlePaymentCallback(callbackRequest);
            return ResponseEntity.ok("Callback processed successfully");
        } catch (Exception e) {
            log.error("Error processing payment callback for transaction ID {}: {}",
                    callbackRequest.transactionId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing callback: " + e.getMessage());
        }
    }
}