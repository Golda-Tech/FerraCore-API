package com.goldatech.collectionsservice.domain.exception;

/**
 * Custom exception for errors encountered when interacting with the external payment gateway.
 */
public class PaymentGatewayException extends RuntimeException {
    private final boolean fatal;

    public PaymentGatewayException(String message) {
        super(message);
        this.fatal = false; // default to non-fatal
    }

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
        this.fatal = false; // default to non-fatal
    }

    public PaymentGatewayException(String message, boolean fatal) {
        super(message);
        this.fatal = fatal;
    }

    public PaymentGatewayException(String message, Throwable cause, boolean fatal) {
        super(message, cause);
        this.fatal = fatal;
    }

    public boolean isFatal() {
        return fatal;
    }
}