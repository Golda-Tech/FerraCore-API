package com.goldatech.collectionsservice.web.controller;

import com.goldatech.collectionsservice.domain.exception.PaymentGatewayException;
import com.goldatech.collectionsservice.domain.service.CollectionService;
import com.goldatech.collectionsservice.web.dto.request.InitiateCollectionRequest;
import com.goldatech.collectionsservice.web.dto.response.CollectionResponse;
import com.goldatech.collectionsservice.domain.exception.IdempotencyConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    /**
     * Initiates a new payment collection.
     * This endpoint receives a request from a client, processes it, and
     * interacts with an external payment gateway to collect funds.
     *
     * @param request The InitiateCollectionRequest containing details for the collection.
     * @return A Mono of ResponseEntity with CollectionResponse, indicating the status of the initiation.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED) // Use ACCEPTED as processing is asynchronous with external API
    public Mono<ResponseEntity<CollectionResponse>> initiateCollection(@RequestBody InitiateCollectionRequest request) {
        // Delegate to the CollectionService to handle the business logic
        return collectionService.initiateCollection(request)
                .map(ResponseEntity::ok)
                .onErrorResume(IdempotencyConflictException.class, e -> {
                    // Handle idempotency conflicts specifically
                    return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(
                            new CollectionResponse(null, e.getReferenceId(), null, null, null, null,
                                    null, null, null, e.getMessage())
                    ));
                })
                .onErrorResume(PaymentGatewayException.class, e -> {
                    // Handle general payment gateway errors
                    return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                            new CollectionResponse(null, null, null, null, null, null,
                                    null, null, null, "Payment gateway error: " + e.getMessage())
                    ));
                })
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.BAD_REQUEST).build()); // Fallback for other issues
    }

    /**
     * Retrieves the details of a specific payment collection by its internal reference ID.
     * This allows clients to check the status of a previously initiated collection.
     *
     * @param collectionRef The internal reference ID of the collection.
     * @return A Mono of ResponseEntity with CollectionResponse, or a 404 if not found.
     */
    @GetMapping("/{collectionRef}")
    public Mono<ResponseEntity<CollectionResponse>> getCollectionDetails(@PathVariable String collectionRef) {
        return collectionService.getCollectionDetails(collectionRef)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Retrieves the details of a specific payment collection by its external payment gateway ID.
     * This allows clients to check the status using the external reference.
     *
     * @param externalRef The external reference ID provided by the payment gateway.
     * @return A Mono of ResponseEntity with CollectionResponse, or a 404 if not found.
     */
    @GetMapping("/external/{externalRef}")
    public Mono<ResponseEntity<CollectionResponse>> getCollectionDetailsByExternalRef(@PathVariable String externalRef) {
        return collectionService.getCollectionDetailsByExternalRef(externalRef)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
