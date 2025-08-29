package com.goldatech.collectionsservice.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "collections")
public class Collection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Optional client-provided idempotency key
    @Column(name = "client_request_id", unique = true)
    private String clientRequestId;

    // Internal unique reference for this collection (also used as idempotency key to external PG)
    @Column(nullable = false, unique = true)
    private String collectionRef;

    // External reference ID from the payment gateway (if available)
    @Column(unique = true)
    private String externalRef;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private String customerId; // Our internal customer ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CollectionStatus status;

    @Column(nullable = false)
    private LocalDateTime initiatedAt;

    private LocalDateTime updatedAt;
    private String description;
    private String failureReason; // Stores reason for failure, if applicable

    // New fields from external API request for internal tracking
    @Column(name = "payment_channel")
    private String paymentChannel;
    private String provider;
    @Column(name = "merchant_name")
    private String merchantName;
    @Column(name = "external_user_id") // userId from external API
    private String externalUserId;

    // New fields from external API response for detailed tracking
    private BigDecimal fees;
    @Column(name = "provider_status_message")
    private String providerStatusMessage;
    @Column(name = "provider_initiated")
    private Boolean providerInitiated;
    @Column(name = "platform_settled")
    private Boolean platformSettled;
    @Column(name = "external_client_id")
    private String externalClientId;
    @Column(name = "client_logo_url")
    private String clientLogoUrl;
    @Column(name = "client_name")
    private String clientName;
    @Column(columnDefinition = "jsonb") // For storing flexible metadata
    private String metadata; // Store as JSON string
}