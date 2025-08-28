package com.goldatech.authservice.web.exception;

import com.goldatech.authservice.domain.exception.UserNotFoundException;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final URI NOT_FOUND_TYPE = URI.create("https://api.rexpayapi.com/errors/not-found");
    private static final URI ISE_FOUND_TYPE = URI.create("https://api.rexpayapi.com/errors/server-error");
    private static final URI BAD_REQUEST_TYPE = URI.create("https://api.rexpayapi.com/errors/bad-request");
    private static final String SERVICE_NAME = "auth-service";

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

    @ExceptionHandler(UserNotFoundException.class)
    ProblemDetail handleProductNotFoundException(UserNotFoundException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problemDetail.setTitle("Order Not Found");
        problemDetail.setType(NOT_FOUND_TYPE);
        problemDetail.setProperty("service", SERVICE_NAME);
        problemDetail.setProperty("error_category", "Generic");
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
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
