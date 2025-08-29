package com.goldatech.collectionsservice.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goldatech.collectionsservice.domain.exception.PaymentGatewayException;
import com.goldatech.collectionsservice.domain.model.Collection;
import com.goldatech.collectionsservice.domain.model.CollectionStatus;
import com.goldatech.collectionsservice.domain.repository.CollectionRepository;
import com.goldatech.collectionsservice.web.dto.request.ExternalInitiatePaymentRequest;
import com.goldatech.collectionsservice.web.dto.request.ExternalPaymentApiRequest;
import com.goldatech.collectionsservice.domain.exception.IdempotencyConflictException;
import com.goldatech.collectionsservice.web.dto.request.InitiateCollectionRequest;
import com.goldatech.collectionsservice.web.dto.request.PaymentCallbackRequest;
import com.goldatech.collectionsservice.web.dto.response.CollectionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final ExternalPaymentGatewayService externalPaymentGatewayService;
    private final RabbitTemplate rabbitTemplate; // For publishing events to other services
    private final ObjectMapper objectMapper; // For JSON serialization of metadata

    @Value("${collections.service.callback-url}")
    private String serviceCallbackUrl;

    /**
     * Initiates a new payment collection based on a client request.
     *
     * @param request The InitiateCollectionRequest from the client.
     * @return A Mono of CollectionResponse representing the initiated collection.
     * @throws IdempotencyConflictException if a request with the same reference is found but has a different status.
     * @throws PaymentGatewayException if an error occurs when calling the external payment API.
     */
    public Mono<CollectionResponse> initiateCollection(InitiateCollectionRequest request) {
        String collectionRef = UUID.randomUUID().toString(); // Our internal idempotency key
        log.info("Initiating collection with internal reference: {}", collectionRef);

        return Mono.defer(() -> {
            // Check for existing collection with the same reference (client-side idempotency if the client retries with the same reference)
            // For now, we generate a new UUID for each client request, ensuring idempotency from *our* system to the external gateway.
            // If the client needs to send their own idempotency key, that would be passed in InitiateCollectionRequest.

            // 1. Persist the initial collection record in our database
            Collection newCollection = Collection.builder()
                    .collectionRef(collectionRef)
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

            return Mono.fromCallable(() -> collectionRepository.save(newCollection))
                    .flatMap(savedCollection -> {
                        // 2. Prepare request for the external payment API
                        ExternalInitiatePaymentRequest externalApiRequest = new ExternalInitiatePaymentRequest(
                                savedCollection.getCollectionRef(), // Our internal ref as external's idempotency key
                                request.amount(),
                                request.currency(),
                                request.customerId(), // Use our customerId as userId for external API
                                request.paymentChannel(),
                                request.provider(),
                                request.merchantName(),
                                serviceCallbackUrl // Our service's callback URL
                        );

                        // 3. Call the external payment gateway
                        return externalPaymentGatewayService.initiatePayment(externalApiRequest)
                                .flatMap(externalApiResponse -> {
                                    // 4. Update our collection record with external reference and status
                                    savedCollection.setExternalRef(externalApiResponse.externalPaymentReference());
                                    savedCollection.setStatus(mapExternalStatus(externalApiResponse.status()));
                                    savedCollection.setUpdatedAt(LocalDateTime.now());
                                    savedCollection.setExternalClientId(externalApiResponse.clientId());
                                    savedCollection.setProviderStatusMessage(externalApiResponse.providerResponse());
                                    // providerExtraInfo and other fields might be mapped here if relevant to Collection entity

                                    return Mono.fromCallable(() -> collectionRepository.save(savedCollection))
                                            .map(this::toCollectionResponse);
                                })
                                .onErrorResume(e -> {
                                    log.error("Error calling external payment API for {}: {}", collectionRef, e.getMessage());
                                    // Update collection status to FAILED if external call fails
                                    savedCollection.setStatus(CollectionStatus.FAILED);
                                    savedCollection.setFailureReason("External API call failed: " + e.getMessage());
                                    savedCollection.setUpdatedAt(LocalDateTime.now());
                                    return Mono.fromCallable(() -> collectionRepository.save(savedCollection))
                                            .map(this::toCollectionResponse)
                                            .then(Mono.error(new PaymentGatewayException("Failed to initiate payment with external gateway for " + collectionRef, e)));
                                });
                    })
                    .doOnError(e -> log.error("Error initiating collection: {}", e.getMessage()));
        });
    }

    /**
     * Retrieves the details of a collection by its internal reference.
     *
     * @param collectionRef The internal reference ID.
     * @return A Mono of CollectionResponse, or empty if not found.
     */
    public Mono<CollectionResponse> getCollectionDetails(String collectionRef) {
        return Mono.fromCallable(() -> collectionRepository.findByCollectionRef(collectionRef))
                .flatMap(optionalCollection -> optionalCollection
                        .map(Mono::just)
                        .orElse(Mono.empty()))
                .flatMap(this::fetchAndMergeExternalDetails) // Fetch external details and merge
                .map(this::toCollectionResponse)
                .doOnError(e -> log.error("Error getting collection details for {}: {}", collectionRef, e.getMessage()));
    }

    /**
     * Retrieves the details of a collection by its external payment gateway reference.
     *
     * @param externalRef The external reference ID.
     * @return A Mono of CollectionResponse, or empty if not found.
     */
    public Mono<CollectionResponse> getCollectionDetailsByExternalRef(String externalRef) {
        return Mono.fromCallable(() -> collectionRepository.findByExternalRef(externalRef))
                .flatMap(optionalCollection -> optionalCollection
                        .map(Mono::just)
                        .orElse(Mono.empty()))
                .flatMap(this::fetchAndMergeExternalDetails) // Fetch external details and merge
                .map(this::toCollectionResponse)
                .doOnError(e -> log.error("Error getting collection details by external ref {}: {}", externalRef, e.getMessage()));
    }

    /**
     * Fetches details from the external payment gateway and merges them into the internal Collection object.
     * This ensures the most up-to-date status and information.
     *
     * @param collection The internal Collection entity.
     * @return A Mono of the updated Collection entity.
     */
    private Mono<Collection> fetchAndMergeExternalDetails(Collection collection) {
        if (collection.getExternalRef() == null) {
            log.warn("Cannot fetch external details for collectionRef {} as externalRef is null.", collection.getCollectionRef());
            return Mono.just(collection); // No external ref, no external details to fetch
        }

        return externalPaymentGatewayService.getPaymentDetails(collection.getExternalRef())
                .flatMap(externalDetails -> {
                    // Update internal collection with the latest external details
                    collection.setStatus(mapExternalStatus(externalDetails.status()));
                    collection.setUpdatedAt(LocalDateTime.now());
                    collection.setFees(externalDetails.fees());
                    collection.setProviderStatusMessage(externalDetails.statusMessage());
                    collection.setProviderInitiated(externalDetails.providerInitiated());
                    collection.setPlatformSettled(externalDetails.platformSettled());
                    collection.setExternalUserId(externalDetails.userId()); // Update if different from our customerId
                    collection.setExternalClientId(externalDetails.clientId());
                    collection.setClientLogoUrl(externalDetails.clientLogo());
                    collection.setClientName(externalDetails.clientName());
                    try {
                        collection.setMetadata(objectMapper.writeValueAsString(externalDetails.metadata()));
                    } catch (JsonProcessingException e) {
                        log.error("Failed to serialize metadata for collection {}: {}", collection.getCollectionRef(), e.getMessage());
                    }

                    return Mono.fromCallable(() -> collectionRepository.save(collection));
                })
                .onErrorResume(e -> {
                    log.error("Failed to fetch external details for collectionRef {}: {}", collection.getCollectionRef(), e.getMessage());
                    // Don't fail the entire get details request if external call fails, return current internal state
                    return Mono.just(collection);
                });
    }

    /**
     * Handles payment status updates received from the external payment gateway's callback.
     *
     * @param callbackRequest The PaymentCallbackRequest from the external gateway.
     * @return A Mono<Void> indicating completion.
     */
    public Mono<Void> handlePaymentCallback(PaymentCallbackRequest callbackRequest) {
        log.info("Processing callback for clientTransactionId: {}", callbackRequest.transactionId());
        return Mono.fromCallable(() -> collectionRepository.findByCollectionRef(callbackRequest.transactionId()))
                .flatMap(optionalCollection -> {
                    if (optionalCollection.isEmpty()) {
                        log.warn("Collection with clientTransactionId {} not found for callback.", callbackRequest.transactionId());
                        // Important: Respond with a success to the external gateway to prevent retries
                        return Mono.empty();
                    }
                    Collection collection = optionalCollection.get();

                    // Check for idempotency: if the status is already final and successful, just log and return
                    if (collection.getStatus() == CollectionStatus.SUCCESS &&
                            mapExternalStatus(callbackRequest.status()) == CollectionStatus.SUCCESS) {
                        log.warn("Ignoring duplicate successful callback for collectionRef {}", collection.getCollectionRef());
                        return Mono.empty();
                    }

                    collection.setExternalRef(callbackRequest.externalTransactionId());
                    collection.setStatus(mapExternalStatus(callbackRequest.status()));
                    collection.setUpdatedAt(LocalDateTime.now());
                    collection.setFailureReason(callbackRequest.reason()); // Store failure reason if provided

                    return Mono.fromCallable(() -> collectionRepository.save(collection))
                            .doOnSuccess(updatedCollection -> {
                                log.info("Collection {} updated to status {}.", updatedCollection.getCollectionRef(), updatedCollection.getStatus());
                                // Publish an event to RabbitMQ for other services (e.g., Notification, Analytics)
                                publishCollectionEvent(updatedCollection);
                            })
                            .then(); // Return Mono<Void>
                })
                .doOnError(e -> log.error("Failed to process payment callback for clientTransactionId {}: {}",
                        callbackRequest.transactionId(), e.getMessage()))
                .then(); // Ensure a Mono<Void> is always returned
    }

    /**
     * Maps an external payment gateway status string to our internal CollectionStatus enum.
     * You will need to implement this logic based on your specific payment provider's documentation.
     *
     * @param externalStatus The status string from the external API.
     * @return The corresponding internal CollectionStatus.
     */
    private CollectionStatus mapExternalStatus(String externalStatus) {
        return switch (externalStatus.toUpperCase()) {
            case "PENDING", "PROCESSING" -> CollectionStatus.PENDING_EXTERNAL;
            case "ONGOING" -> CollectionStatus.ONGOING; // New status for in-progress
            case "SUCCESS", "COMPLETED" -> CollectionStatus.SUCCESS;
            case "FAILED", "DECLINED", "ERROR" -> CollectionStatus.FAILED;
            case "CANCELLED" -> CollectionStatus.CANCELLED;
            case "REFUNDED" -> CollectionStatus.REFUNDED;
            default -> CollectionStatus.INITIATED; // Default or handle as unknown
        };
    }

    /**
     * Converts a Collection entity to a CollectionResponse DTO.
     * @param collection The Collection entity.
     * @return The corresponding CollectionResponse DTO.
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
                "Collection status: " + collection.getStatus().name() + (collection.getProviderStatusMessage() != null ? " (" + collection.getProviderStatusMessage() + ")" : "")
        );
    }

    /**
     * Publishes a collection event to RabbitMQ.
     * This method sends a message to a RabbitMQ queue/exchange, which can then be consumed
     * by other microservices like the Notification Service or Analytics Service.
     *
     * @param collection The updated Collection entity.
     */
    private void publishCollectionEvent(Collection collection) {
        // You'll define the exchange and routing key for your RabbitMQ setup.
        // For simplicity, we'll use a direct exchange with a routing key based on status.
        String exchangeName = "payment.events";
        String routingKey = "collection." + collection.getStatus().name().toLowerCase();
        log.info("Publishing collection event for {}: Status {}", collection.getCollectionRef(), collection.getStatus());

        // The object sent through RabbitMQ should ideally be a dedicated DTO
        // that contains only the necessary information for the event consumer.
        // For example: new CollectionEventDTO(collection.getCollectionRef(), collection.getStatus(), ...)
        rabbitTemplate.convertAndSend(exchangeName, routingKey, collection);
    }
}