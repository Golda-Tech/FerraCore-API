package com.goldatech.apigateway.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtService jwtService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/v1/auth/**",
            "/api/v1/health/**",
            "/actuator/health/**",

            // Swagger & OpenAPI docs
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**",

            // Proxied Swagger docs through gateway
            "/auth-service-docs/**",
            "/collections-service-docs/**"
    );


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        log.debug("Processing request to path: {}", path);

        // Skip authentication for public endpoints
        if (isPublicEndpoint(path)) {
            log.debug("Skipping authentication for public endpoint: {}", path);
            return chain.filter(exchange);
        }

        // Extract Authorization header
        String authHeader = request.getHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            return handleUnauthorized(exchange, "Missing or invalid Authorization header");
        }

        String jwt = authHeader.substring(7);

        // Validate token and extract user info
        return jwtService.validateTokenAndExtractInfo(jwt)
                .flatMap(tokenInfo -> {
                    log.debug("Token validated successfully for user: {}", tokenInfo.getUsername());

                    // Add user info headers to the request
                    ServerHttpRequest mutatedRequest = request.mutate()
                            .header("X-User-Email", tokenInfo.getUsername())
                            .header("X-User-Roles", tokenInfo.getRoles())
                            .header("X-User-Id", tokenInfo.getUserId())
                            .header("X-Gateway-Verified", "true")
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                })
                .onErrorResume(error -> {
                    log.error("Authentication failed for path {}: {}", path, error.getMessage());
                    return handleUnauthorized(exchange, "Invalid or expired token");
                });
    }

    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");

        String errorResponse = String.format(
                "{\"timestamp\":\"%s\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"%s\",\"path\":\"%s\"}",
                java.time.LocalDateTime.now(),
                message,
                exchange.getRequest().getPath().value()
        );

        org.springframework.core.io.buffer.DataBuffer buffer = response.bufferFactory().wrap(errorResponse.getBytes());
        return response.writeWith(Mono.just(buffer));
    }

    private boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
