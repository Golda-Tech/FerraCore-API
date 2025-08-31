package com.goldatech.apigateway.controller;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @Value("${spring.application.name:api-gateway}")
    private String applicationName;

    @GetMapping
    public Mono<Map<String, Object>> health() {
        return Mono.fromCallable(() -> {
            Map<String, Object> health = new HashMap<>();
            health.put("service", applicationName);
            health.put("status", "UP");
            health.put("timestamp", LocalDateTime.now());
            health.put("version", "1.0.0");

            Map<String, Object> dependencies = new HashMap<>();
            dependencies.put("auth-service", "https://auth-service-9tm1.onrender.com");
            dependencies.put("collections-service", "https://collections-service.onrender.com");
            health.put("dependencies", dependencies);

            return health;
        });
    }

    @GetMapping("/ready")
    public Mono<Map<String, Object>> ready() {
        return Mono.fromCallable(() -> {
            Map<String, Object> readiness = new HashMap<>();
            readiness.put("status", "READY");
            readiness.put("timestamp", LocalDateTime.now());
            return readiness;
        });
    }
}