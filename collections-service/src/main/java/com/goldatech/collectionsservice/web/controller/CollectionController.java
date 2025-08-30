package com.goldatech.collectionsservice.web.controller;

import com.goldatech.collectionsservice.domain.exception.PaymentGatewayException;
import com.goldatech.collectionsservice.domain.service.CollectionService;
import com.goldatech.collectionsservice.web.dto.request.InitiateCollectionRequest;
import com.goldatech.collectionsservice.web.dto.response.CollectionResponse;
import com.goldatech.collectionsservice.domain.exception.IdempotencyConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/collections")
@RequiredArgsConstructor
@Slf4j
public class CollectionController {

    private final CollectionService collectionService;

    /**
     * Initiates a new payment collection.
     */
    @PostMapping
    public ResponseEntity<CollectionResponse> initiateCollection(@RequestBody InitiateCollectionRequest request) {
        try {
            return collectionService.initiateCollection(request);
        } catch (IdempotencyConflictException e) {
            CollectionResponse existingCollection = collectionService.getCollectionDetails(e.getReferenceId());
            if (existingCollection != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(existingCollection);
            } else {
                CollectionResponse errorResponse = new CollectionResponse(
                        null, e.getReferenceId(), null, null, null, null,
                        null, null, null, e.getMessage()
                );
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            }
        } catch (PaymentGatewayException e) {
            CollectionResponse errorResponse = new CollectionResponse(
                    null, null, null, null, null, null,
                    null, null, null, "Payment gateway error: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        } catch (Exception e) {
            log.error("Unexpected error initiating collection: {}", e.getMessage(), e);
            CollectionResponse errorResponse = new CollectionResponse(
                    null, null, null, null, null, null,
                    null, null, null, "Internal server error"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Retrieves the details of a specific payment collection by its internal reference ID.
     */
    @GetMapping("/{collectionRef}")
    public ResponseEntity<CollectionResponse> getCollectionDetails(@PathVariable String collectionRef) {
        try {
            CollectionResponse collection = collectionService.getCollectionDetails(collectionRef);
            if (collection != null) {
                return ResponseEntity.ok(collection);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error getting collection details for {}: {}", collectionRef, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Retrieves the details of a specific payment collection by its external payment gateway ID.
     */
    @GetMapping("/external/{externalRef}")
    public ResponseEntity<CollectionResponse> getCollectionDetailsByExternalRef(@PathVariable String externalRef) {
        try {
            CollectionResponse collection = collectionService.getCollectionDetailsByExternalRef(externalRef);
            if (collection != null) {
                return ResponseEntity.ok(collection);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error getting collection details by external ref {}: {}", externalRef, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}