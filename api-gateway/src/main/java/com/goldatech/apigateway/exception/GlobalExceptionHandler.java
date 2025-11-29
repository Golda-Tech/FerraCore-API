package com.goldatech.apigateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
@Order(-2)
@Slf4j
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private static final List<String> SECURITY_PROBE_PATTERNS = Arrays.asList(
            ".git", ".env", ".config", ".aws", ".docker",
            "config.json", ".htaccess", "web.config", ".svn"
    );

    private static final List<String> COMMON_STATIC_PATHS = Arrays.asList(
            "/robots.txt", "/favicon.ico", "/sitemap.xml"
    );

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        String path = exchange.getRequest().getPath().value();
        HttpStatus status;
        String message;

        // Handle NoResourceFoundException (404 errors)
        if (ex instanceof NoResourceFoundException) {
            status = HttpStatus.NOT_FOUND;
            message = "The requested resource was not found";

            // Log at different levels based on request type
            if (isSecurityProbe(path)) {
                log.debug("Security probe attempt blocked: {} from IP: {}",
                        path, getClientIp(exchange));
            } else if (isCommonStaticPath(path)) {
                log.debug("Common static resource requested: {}", path);
            } else {
                log.warn("Resource not found: {}", path);
            }
        }
        // Handle specific exceptions
        else if (ex instanceof RuntimeException) {
            status = HttpStatus.BAD_REQUEST;
            message = ex.getMessage();
            log.error("Runtime exception in API Gateway: ", ex);
        }
        // Handle all other exceptions
        else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "An unexpected error occurred in the API Gateway";
            log.error("Unexpected error in API Gateway: ", ex);
        }

        // Set response headers
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.setStatusCode(status);

        String errorResponse = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );

        DataBuffer buffer = response.bufferFactory().wrap(errorResponse.getBytes());
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * Checks if the request path matches known security probe patterns
     */
    private boolean isSecurityProbe(String path) {
        String lowerPath = path.toLowerCase();
        return SECURITY_PROBE_PATTERNS.stream()
                .anyMatch(lowerPath::contains);
    }

    /**
     * Checks if the request is for a common static resource
     */
    private boolean isCommonStaticPath(String path) {
        return COMMON_STATIC_PATHS.contains(path);
    }

    /**
     * Extracts the client IP address from the request
     */
    private String getClientIp(ServerWebExchange exchange) {
        // Check X-Forwarded-For header first (for requests behind proxy/load balancer)
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        // Check X-Real-IP header
        String xRealIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        // Fall back to remote address
        if (exchange.getRequest().getRemoteAddress() != null) {
            return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }

        return "unknown";
    }
}