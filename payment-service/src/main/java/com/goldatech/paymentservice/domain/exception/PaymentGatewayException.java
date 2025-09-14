package com.goldatech.paymentservice.domain.exception;

import lombok.Getter;

@Getter
public class PaymentGatewayException extends RuntimeException {

    public PaymentGatewayException(String message) {
        super(message);
    }

    public PaymentGatewayException(String message, Throwable cause, boolean isFatal) {
        super(message, cause);
        this.isFatal = isFatal;
    }

    private boolean isFatal;

    public PaymentGatewayException(String unexpectedErrorInRequestToPay, Exception e) {
    }

}
