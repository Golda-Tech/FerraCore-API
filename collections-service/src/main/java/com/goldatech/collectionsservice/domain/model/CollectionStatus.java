package com.goldatech.collectionsservice.domain.model;

public enum CollectionStatus {
    INITIATED,      // Collection request received by our service
    PENDING_EXTERNAL, // Sent to external gateway, awaiting first response
    ONGOING,        // External payment processing is in progress
    SUCCESS,        // Payment successfully collected
    FAILED,         // Payment failed
    CANCELLED,      // Collection was cancelled
    REFUNDED        // Funds were returned (might trigger a disbursement)
}
