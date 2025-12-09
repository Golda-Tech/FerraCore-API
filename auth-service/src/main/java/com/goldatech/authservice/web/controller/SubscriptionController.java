package com.goldatech.authservice.web.controller;

import com.goldatech.authservice.domain.service.SubscriptionService;
import com.goldatech.authservice.web.dto.request.SubscriptionCreateRequest;
import com.goldatech.authservice.web.dto.request.SubscriptionLoginRequest;
import com.goldatech.authservice.web.dto.request.SubscriptionUpdateRequest;
import com.goldatech.authservice.web.dto.response.ApiCredentialsResponse;
import com.goldatech.authservice.web.dto.response.SubscriptionAuthResponse;
import com.goldatech.authservice.web.dto.response.SubscriptionResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {



    private final SubscriptionService service;

    public SubscriptionController(SubscriptionService service) {
        this.service = service;
    }

    @PostMapping("/initiate")
    public SubscriptionResponse create(@Valid @RequestBody SubscriptionCreateRequest request) {
        log.info("Received request to create subscription for org: {}", request.organizationName());
        return service.createSubscription(request);
    }

    @GetMapping
    public List<SubscriptionResponse> getAll() {
        log.debug("Received request to fetch all subscriptions");
        return service.getAllSubscriptions();
    }

    @PostMapping("/tokens")
    public SubscriptionAuthResponse authorizeAccessToken(@RequestHeader("Authorization") String authorization, @RequestBody SubscriptionLoginRequest request) {
        log.info("Received subscription auth request with key: {}", request.subscriptionKey());
        return service.authorize(request, authorization);
    }

    @GetMapping("/{key}")
    public SubscriptionResponse getByKey(@Valid @PathVariable String key) {
        log.debug("Received request to fetch subscription by key: {}", key);
        return service.getByKey(key)
                .orElseThrow(() -> {
                    log.error("Subscription not found with key: {}", key);
                    return new RuntimeException("Subscription not found");
                });
    }

    @PutMapping("/{id}")
    public SubscriptionResponse update(@PathVariable Long id,
                                       @RequestBody SubscriptionUpdateRequest request) {
        log.info("Received request to update subscription with id: {}", id);
        return service.updateSubscription(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        log.warn("Received request to delete subscription with id: {}", id);
        service.deleteSubscription(id);
    }

    /**
     * Regenerates API credentials for a subscription.
     * WARNING: This will invalidate the current credentials.
     *
     * @param id the subscription ID
     * @return SubscriptionResponse with new credentials
     */
    @PostMapping("/{id}/regenerate-credentials")
    public SubscriptionResponse regenerateCredentials(@PathVariable Long id) {
        log.warn("Received request to regenerate credentials for subscription ID: {}", id);
        return service.regenerateCredentials(id);
    }

    /**
     * Gets only the API credentials for a subscription.
     *
     * @param id the subscription ID
     * @return ApiCredentialsResponse containing key and secret
     */
    @GetMapping("/{id}/credentials")
    public ApiCredentialsResponse getCredentials(@PathVariable Long id) {
        log.debug("Received request to fetch credentials for subscription ID: {}", id);
        return service.getCredentials(id);
    }

    /**
     * Regenerates credentials using the current subscription key.
     * This is useful when called from an authenticated context where you know the key.
     *
     * @param key the current subscription key
     * @return SubscriptionResponse with new credentials
     */
    @PostMapping("/regenerate-credentials")
    public SubscriptionResponse regenerateCredentialsByKey(@RequestParam String key) {
        log.warn("Received request to regenerate credentials for subscription key: {}", key);
        return service.regenerateCredentialsByKey(key);
    }
}
