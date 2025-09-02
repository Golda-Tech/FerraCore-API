package com.goldatech.collectionsservice.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goldatech.collectionsservice.domain.exception.PaymentGatewayException;
import com.goldatech.collectionsservice.domain.model.Collection;
import com.goldatech.collectionsservice.domain.model.CollectionStatus;
import com.goldatech.collectionsservice.domain.repository.CollectionRepository;
import com.goldatech.collectionsservice.web.dto.request.ExternalInitiatePaymentRequest;
import com.goldatech.collectionsservice.domain.exception.IdempotencyConflictException;
import com.goldatech.collectionsservice.web.dto.request.InitiateCollectionRequest;
import com.goldatech.collectionsservice.web.dto.request.PaymentCallbackRequest;
import com.goldatech.collectionsservice.web.dto.response.CollectionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final ExternalPaymentGatewayService externalPaymentGatewayService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${collections.service.callback-url}")
    private String serviceCallbackUrl;

    /**
     * Initiates a new payment collection based on a client request.
     * Implements client-side idempotency using the `clientRequestId`.
     *
     * @param request The InitiateCollectionRequest from the client, including an optional `clientRequestId`.
     * @return A ResponseEntity with CollectionResponse representing the initiated collection.
     * @throws IdempotencyConflictException if a request with the same `clientRequestId` is found
     * and its status is not `INITIATED` or `FAILED`.
     * @throws PaymentGatewayException      if an error occurs when calling the external payment API.
     */
    @Transactional
    public ResponseEntity<CollectionResponse> initiateCollection(InitiateCollectionRequest request) {
        final String clientProvidedIdempotencyKey = request.clientRequestId();
        final String internalCollectionRef = UUID.randomUUID().toString();

        final String sanitizedInternalRef = sanitizeReference(internalCollectionRef);

        log.info("Initiating collection with clientRequestId: {} (internal ref: {})",
                clientProvidedIdempotencyKey, sanitizedInternalRef);

        // Check for existing collection based on clientRequestId (if provided)
        if (clientProvidedIdempotencyKey != null) {
            Optional<Collection> existingCollectionOptional =
                    collectionRepository.findByClientRequestId(clientProvidedIdempotencyKey);

            if (existingCollectionOptional.isPresent()) {
                Collection existingCollection = existingCollectionOptional.get();

                if (existingCollection.getStatus() != CollectionStatus.INITIATED &&
                        existingCollection.getStatus() != CollectionStatus.FAILED) {
                    log.warn("Idempotent request received with clientRequestId: {}. Returning existing collection details.",
                            clientProvidedIdempotencyKey);
                    throw new IdempotencyConflictException(clientProvidedIdempotencyKey,
                            "A collection with this clientRequestId is already being processed or has completed.");
                }

                log.info("Idempotent request for clientRequestId {} found in state {}. Attempting to re-process.",
                        clientProvidedIdempotencyKey, existingCollection.getStatus());
                return processCollection(existingCollection, request);
            }
        }

        // Create new collection
        Collection newCollection = Collection.builder()
                .clientRequestId(clientProvidedIdempotencyKey)
                .collectionRef(sanitizedInternalRef)
                .amount(request.amount())
                .currency(request.currency())
                .customerId(request.customerId())
                .status(CollectionStatus.INITIATED)
                .initiatedAt(LocalDateTime.now())
                .description(request.description())
                .paymentChannel(request.paymentChannel())
                .provider(request.provider())
                .merchantName(request.merchantName())
                .build();

        return processCollection(newCollection, request);
    }

    /**
     * Helper method to process (save and call external API) a collection.
     */
    private ResponseEntity<CollectionResponse> processCollection(Collection collection, InitiateCollectionRequest request) {
        try {
            Collection savedCollection = collectionRepository.save(collection);

            ExternalInitiatePaymentRequest externalApiRequest = new ExternalInitiatePaymentRequest(
                    savedCollection.getCollectionRef(),
                    request.amount(),
                    request.currency(),
                    request.customerId(),
                    request.paymentChannel(),
                    request.provider(),
                    request.merchantName(),
                    serviceCallbackUrl
            );

            try {
                var externalApiResponse = externalPaymentGatewayService.initiatePayment(externalApiRequest);

                savedCollection.setExternalRef(externalApiResponse.externalPaymentReference());
                savedCollection.setStatus(mapExternalStatus(externalApiResponse.status()));
                savedCollection.setUpdatedAt(LocalDateTime.now());
                savedCollection.setExternalClientId(externalApiResponse.clientId());
                savedCollection.setProviderStatusMessage(externalApiResponse.providerResponse());

                savedCollection = collectionRepository.save(savedCollection);
                return ResponseEntity.ok(toCollectionResponse(savedCollection));

            } catch (Exception e) {
                log.error("Error calling external payment API for {}: {}", savedCollection.getCollectionRef(), e.getMessage());
                savedCollection.setStatus(CollectionStatus.FAILED);
                savedCollection.setFailureReason("External API call failed: " + e.getMessage());
                savedCollection.setUpdatedAt(LocalDateTime.now());

                collectionRepository.save(savedCollection);
                throw new PaymentGatewayException("Failed to initiate payment with external gateway for " +
                        savedCollection.getCollectionRef(), e);
            }
        } catch (Exception e) {
            log.error("Error in processCollection: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Retrieves the details of a collection by its internal reference.
     */
    public CollectionResponse getCollectionDetails(String collectionRef) {
        Optional<Collection> collectionOptional = collectionRepository.findByCollectionRef(collectionRef);

        if (collectionOptional.isEmpty()) {
            return null;
        }

        Collection collection = fetchAndMergeExternalDetails(collectionOptional.get());
        return toCollectionResponse(collection);
    }

    /**
     * Retrieves the details of a collection by its external payment gateway reference.
     */
    public CollectionResponse getCollectionDetailsByExternalRef(String externalRef) {
        Optional<Collection> collectionOptional = collectionRepository.findByExternalRef(externalRef);

        if (collectionOptional.isEmpty()) {
            return null;
        }

        Collection collection = fetchAndMergeExternalDetails(collectionOptional.get());
        return toCollectionResponse(collection);
    }

    /**
     * Fetches details from the external payment gateway and merges them into the internal Collection object.
     */
    private Collection fetchAndMergeExternalDetails(Collection collection) {
        if (collection.getExternalRef() == null) {
            log.warn("Cannot fetch external details for collectionRef {} as externalRef is null.",
                    collection.getCollectionRef());
            return collection;
        }

        try {
            var externalDetails = externalPaymentGatewayService.getPaymentDetails(collection.getCollectionRef());

            collection.setStatus(mapExternalStatus(externalDetails.status()));
            collection.setUpdatedAt(LocalDateTime.now());
            collection.setFees(externalDetails.fees());
            collection.setProviderStatusMessage(externalDetails.statusMessage());
            collection.setProviderInitiated(externalDetails.providerInitiated());
            collection.setPlatformSettled(externalDetails.platformSettled());
            collection.setExternalUserId(externalDetails.userId());
            collection.setExternalClientId(externalDetails.clientId());
            collection.setClientLogoUrl(externalDetails.clientLogo());
            collection.setClientName(externalDetails.clientName());

            try {
                collection.setMetadata(objectMapper.writeValueAsString(externalDetails.metadata()));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize metadata for collection {}: {}",
                        collection.getCollectionRef(), e.getMessage());
            }

            return collectionRepository.save(collection);

        } catch (Exception e) {
            log.error("Failed to fetch external details for collectionRef {}: {}",
                    collection.getCollectionRef(), e.getMessage());
            return collection; // Return current state if external call fails
        }
    }

    /**
     * Handles payment status updates received from the external payment gateway's callback.
     */
    @Transactional
    public void handlePaymentCallback(PaymentCallbackRequest callbackRequest) {
        log.info("Processing callback for clientTransactionId: {}", callbackRequest.transactionId());

        Optional<Collection> collectionOptional =
                collectionRepository.findByCollectionRef(callbackRequest.transactionId());

        if (collectionOptional.isEmpty()) {
            log.warn("Collection with clientTransactionId {} not found for callback.",
                    callbackRequest.transactionId());
            return;
        }

        Collection collection = collectionOptional.get();

        // Check for idempotency: if the status is already final and successful, just log and return
        if (collection.getStatus() == CollectionStatus.SUCCESS &&
                mapExternalStatus(callbackRequest.status()) == CollectionStatus.SUCCESS) {
            log.warn("Ignoring duplicate successful callback for collectionRef {}", collection.getCollectionRef());
            return;
        }

        collection.setExternalRef(callbackRequest.externalTransactionId());
        collection.setStatus(mapExternalStatus(callbackRequest.status()));
        collection.setUpdatedAt(LocalDateTime.now());
        collection.setFailureReason(callbackRequest.reason());

        Collection updatedCollection = collectionRepository.save(collection);
        log.info("Collection {} updated to status {}.", updatedCollection.getCollectionRef(), updatedCollection.getStatus());

        publishCollectionEvent(updatedCollection);
    }

    /**
     * Retrieves all collections.
     * @return a list of all collections.
     */
    public List<CollectionResponse> getCollections(LocalDateTime startDate,
                                                   LocalDateTime endDate,
                                                   String status,
                                                   Integer page,
                                                   Integer size) {
        Pageable pageable = PageRequest.of(page != null ? page : 0,
                size != null ? size : 20,
                Sort.by(Sort.Direction.DESC, "initiatedAt"));

        Page<Collection> collPage;

        if (startDate != null && endDate != null && status != null) {
            collPage = collectionRepository.findByInitiatedAtBetweenAndStatus(
                    startDate, endDate, CollectionStatus.valueOf(status.toUpperCase()), pageable
            );
        } else if (startDate != null && endDate != null) {
            collPage = collectionRepository.findByInitiatedAtBetween(startDate, endDate, pageable);
        } else if (status != null) {
            collPage = collectionRepository.findByStatus(CollectionStatus.valueOf(status.toUpperCase()), pageable);
        } else {
            collPage = collectionRepository.findAll(pageable);
        }

        return collPage.stream().map(this::toCollectionResponse).toList();
    }

    /**
     * Provides a summary count of collections by their status.
     * @return A map with collection statuses as keys and their respective counts as values.
     */
    public Map<String, Long> getCollectionStatusSummary() {
        return collectionRepository.findAll().stream()
                .collect(Collectors.groupingBy(c -> c.getStatus().name(), Collectors.counting()));
    }


    /**
     * Maps an external payment gateway status string to our internal CollectionStatus enum.
     */
    private CollectionStatus mapExternalStatus(String externalStatus) {
        return switch (externalStatus.toUpperCase()) {
            case "PENDING", "PROCESSING" -> CollectionStatus.PENDING_EXTERNAL;
            case "ONGOING" -> CollectionStatus.ONGOING;
            case "SUCCESS", "COMPLETED" -> CollectionStatus.SUCCESS;
            case "FAILED", "DECLINED", "ERROR" -> CollectionStatus.FAILED;
            case "CANCELLED" -> CollectionStatus.CANCELLED;
            case "REFUNDED" -> CollectionStatus.REFUNDED;
            default -> CollectionStatus.INITIATED;
        };
    }

    /**
     * Converts a Collection entity to a CollectionResponse DTO.
     */
    private CollectionResponse toCollectionResponse(Collection collection) {
        return new CollectionResponse(
                collection.getId(),
                collection.getCollectionRef(),
                collection.getExternalRef(),
                collection.getAmount(),
                collection.getCurrency(),
                collection.getCustomerId(),
                collection.getStatus(),
                collection.getInitiatedAt(),
                collection.getUpdatedAt(),
                "Collection status: " + collection.getStatus().name() +
                        (collection.getProviderStatusMessage() != null ? " (" + collection.getProviderStatusMessage() + ")" : "")
        );
    }

    /**
     * Publishes a collection event to RabbitMQ.
     */
    private void publishCollectionEvent(Collection collection) {
        String exchangeName = "payment.events";
        String routingKey = "collection." + collection.getStatus().name().toLowerCase();
        log.info("Publishing collection event for {}: Status {}", collection.getCollectionRef(), collection.getStatus());

        rabbitTemplate.convertAndSend(exchangeName, routingKey, collection);
    }

    /**
     * Sanitizes a reference string to contain only alphanumeric characters.
     */
    private String sanitizeReference(String reference) {
        if (reference == null) {
            return "";
        }
        return reference.replaceAll("[^a-zA-Z0-9]", "");
    }
}