package com.goldatech.apigateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final WebClient webClient = WebClient.builder()
            .build();

    @Value("${spring.application.name:api-gateway}")
    private String applicationName;

    @Value("${AUTH_SERVICE_URL:http://localhost:8081}")
    private String authServiceUrl;

    @Value("${COLLECTIONS_SERVICE_URL:http://localhost:8082}")
    private String collectionsServiceUrl;

    @Value("${PAYMENTS_SERVICE_URL:http://localhost:8083}")
    private String paymentsServiceUrl;

    @GetMapping
    public Mono<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("service", applicationName);
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("version", "1.0.0");

        Mono<Map<String, Object>> deps = Mono.zip(
                checkService("auth-service", authServiceUrl + "/actuator/health"),
                checkService("collections-service", collectionsServiceUrl + "/actuator/health"),
                checkService("payments-service", paymentsServiceUrl + "/actuator/health")
        ).map(tuple -> {
            Map<String, Object> dependencies = new HashMap<>();
            dependencies.putAll(tuple.getT1());
            dependencies.putAll(tuple.getT2());
            dependencies.putAll(tuple.getT3());
            return dependencies;
        });

        return deps.map(dependencies -> {
            health.put("dependencies", dependencies);
            return health;
        });
    }

    private Mono<Map<String, String>> checkService(String name, String url) {
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(3)) // ⏱ timeout safeguard
                .map(body -> {
                    log.info("Dependency {} is UP at {}", name, url);
                    return Map.of(name, "UP");
                })
                .onErrorResume(e -> {
                    log.warn("Dependency {} is DOWN at {} - reason: {}", name, url, e.getMessage());
                    return Mono.just(Map.of(name, "DOWN"));
                });
    }
}
