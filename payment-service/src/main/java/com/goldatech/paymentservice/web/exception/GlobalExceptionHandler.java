package com.goldatech.paymentservice.web.exception;

import com.goldatech.paymentservice.domain.exception.PaymentGatewayException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final URI NOT_FOUND_TYPE = URI.create("https://ferracore.tech/errors/not-found");
    private static final URI ISE_FOUND_TYPE = URI.create("https://ferracore.tech/errors/server-error");
    private static final URI BAD_REQUEST_TYPE = URI.create("https://ferracore.tech/errors/bad-request");
    private static final URI CONFLICT_TYPE = URI.create("https://ferracore.tech/errors/conflict");
    private static final String SERVICE_NAME = "payment-service";

    @ExceptionHandler(MissingRequestHeaderException.class)
    ProblemDetail handleMissingRequestHeader(MissingRequestHeaderException e) {
        String message = String.format("Required header '%s' is missing", e.getHeaderName());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
        problemDetail.setTitle("Missing Required Header");
        problemDetail.setType(BAD_REQUEST_TYPE);
        problemDetail.setProperty("service", SERVICE_NAME);
        problemDetail.setProperty("error_category", "Validation");
        problemDetail.setProperty("missing_header", e.getHeaderName());
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnhandledException(Exception e) {
        System.out.println("Unhandled exception occurred: " + e.getMessage());
        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(ISE_FOUND_TYPE);
        problemDetail.setProperty("service", SERVICE_NAME);
        problemDetail.setProperty("error_category", "Generic");
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(PaymentGatewayException.class)
    ProblemDetail handlePaymentGatewayException(PaymentGatewayException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, e.getMessage());
        problemDetail.setTitle("Payment Gateway Error");
        problemDetail.setType(BAD_REQUEST_TYPE);
        problemDetail.setProperty("service", SERVICE_NAME);
        problemDetail.setProperty("error_category", "Payment");
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @Override
    @Nullable
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Required request body is missing or malformed");
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(BAD_REQUEST_TYPE);
        problemDetail.setProperty("service", SERVICE_NAME);
        problemDetail.setProperty("error_category", "Validation");
        problemDetail.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @Override
    @Nullable
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<String> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            String field = error.getField();
            Object rejectedValue = error.getRejectedValue();
            String message = error.getDefaultMessage();
            errors.add(field + ": '" + rejectedValue + "' - " + message);
        });

        ProblemDetail problemDetail =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request payload");
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(BAD_REQUEST_TYPE);
        problemDetail.setProperty("errors", errors);
        problemDetail.setProperty("service", SERVICE_NAME);
        problemDetail.setProperty("error_category", "Generic");
        problemDetail.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }
}