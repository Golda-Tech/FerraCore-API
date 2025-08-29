package com.goldatech.collectionsservice.domain.exception;

import lombok.Getter;

/**
 * Custom exception for handling idempotency conflicts when initiating payments.
 * This is thrown if a request with a duplicate idempotency key (reference ID) is received
 * but the existing transaction has a different state than expected.
 */
@Getter
public class IdempotencyConflictException extends RuntimeException {
    private final String referenceId;

    public IdempotencyConflictException(String referenceId, String message) {
        super(message);
        this.referenceId = referenceId;
    }

    public IdempotencyConflictException(String referenceId, String message, Throwable cause) {
        super(message, cause);
        this.referenceId = referenceId;
    }
}
