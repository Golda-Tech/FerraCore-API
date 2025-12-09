package com.goldatech.authservice.domain.service;

import com.goldatech.authservice.domain.mapper.SubscriptionMapper;
import com.goldatech.authservice.domain.model.Subscription;
import com.goldatech.authservice.domain.model.SubscriptionStatus;
import com.goldatech.authservice.domain.repository.SubscriptionRepository;
import com.goldatech.authservice.security.JwtService;
import com.goldatech.authservice.web.dto.request.SubscriptionCreateRequest;
import com.goldatech.authservice.web.dto.request.SubscriptionLoginRequest;
import com.goldatech.authservice.web.dto.request.SubscriptionUpdateRequest;
import com.goldatech.authservice.web.dto.response.ApiCredentialsResponse;
import com.goldatech.authservice.web.dto.response.Organization;
import com.goldatech.authservice.web.dto.response.SubscriptionAuthResponse;
import com.goldatech.authservice.web.dto.response.SubscriptionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class SubscriptionService {

    private final SubscriptionRepository repository;
    private final JwtService jwtService;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    public SubscriptionService(SubscriptionRepository repository, JwtService jwtService) {
        this.repository = repository;
        this.jwtService = jwtService;
    }

    public SubscriptionResponse createSubscription(SubscriptionCreateRequest request) {
        log.info("Creating subscription for org: {}", request.organizationName());

        String generatedKey = generateRandomKey();
        String generatedSecret = generateRandomSecret();

        Subscription subscription = Subscription.builder()
                .organizationName(request.organizationName())
                .planType(request.planType())
                .contactEmail(request.contactEmail())
                .subscriptionKey(generatedKey)
                .subscriptionSecret(generatedSecret)
                .status(SubscriptionStatus.valueOf("ACTIVE"))
                .build();

        Subscription saved = repository.save(subscription);
        return SubscriptionMapper.toResponse(saved);
    }


    public SubscriptionAuthResponse authorize(SubscriptionLoginRequest request, String authorization) {
        log.info("Authenticating subscription with key: {}", request.subscriptionKey());

        //verify authorization header - a basic auth header
        if (authorization == null || !authorization.startsWith("Basic ")) {
            log.error("Missing or invalid Authorization header");
            throw new RuntimeException("Missing or invalid Authorization header");
        }
        //validate authorization token by decoding the Base 64 basic auth token
        String base64Credentials = authorization.substring("Basic ".length());
        byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
        String credentials = new String(credDecoded);
        //credentials = subscriptionKey:subscriptionSecret
        String[] values = credentials.split(":", 2);
        if (values.length != 2 || !values[0].equals(request.subscriptionKey()) || !values[1].equals(request.subscriptionSecret())) {
            log.error("Authorization header does not match provided subscription credentials");
            throw new RuntimeException("Authorization header does not match provided subscription credentials");
        }

        var subscription = repository.findBySubscriptionKey(request.subscriptionKey())
                .filter(sub -> sub.getSubscriptionSecret().equals(request.subscriptionSecret()))
                .orElseThrow(() -> new RuntimeException("Invalid subscription credentials"));

        var jwtToken = jwtService.generateToken(subscription);

        return new SubscriptionAuthResponse(
                jwtToken,
                (int) jwtExpiration,
                subscription.getId(),
                new Organization(subscription.getOrganizationName(), subscription.getContactEmail(), subscription.getPlanType(),
                        subscription.getStatus(), "", ""),
                "Subscription authorized successfully."
        );
    }


    public List<SubscriptionResponse> getAllSubscriptions() {
        log.debug("Fetching all subscriptions");
        return repository.findAll().stream()
                .map(SubscriptionMapper::toResponse)
                .toList();
    }

    public Optional<SubscriptionResponse> getByKey(String key) {
        log.debug("Fetching subscription by key: {}", key);
        return repository.findBySubscriptionKey(key)
                .map(SubscriptionMapper::toResponse);
    }

    public SubscriptionResponse updateSubscription(Long id, SubscriptionUpdateRequest request) {
        log.info("Updating subscription with id: {}", id);
        return repository.findById(id)
                .map(existing -> {
                    SubscriptionMapper.updateEntity(existing, request);
                    Subscription updated = repository.save(existing);
                    return SubscriptionMapper.toResponse(updated);
                })
                .orElseThrow(() -> {
                    log.error("Subscription not found with id: {}", id);
                    return new RuntimeException("Subscription not found");
                });
    }

    public void deleteSubscription(Long id) {
        log.warn("Deleting subscription with id: {}", id);
        repository.deleteById(id);
    }

    @Transactional
    public SubscriptionResponse regenerateCredentials(Long subscriptionId) {
        log.warn("Regenerating credentials for subscription ID: {}", subscriptionId);

        Subscription subscription = repository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        // Generate new credentials
        subscription.setSubscriptionKey(generateRandomKey());
        subscription.setSubscriptionSecret(generateRandomSecret());

        Subscription updated = repository.save(subscription);

        log.info("Credentials regenerated successfully for subscription ID: {}", subscriptionId);
        return SubscriptionMapper.toResponse(updated);
    }

    /**
     * Regenerates credentials by subscription key (for authenticated API calls)
     *
     * @param currentKey the current subscription key
     * @return SubscriptionResponse with new credentials
     */
    @Transactional
    public SubscriptionResponse regenerateCredentialsByKey(String currentKey) {
        log.warn("Regenerating credentials for subscription key: {}", currentKey);

        Subscription subscription = repository.findBySubscriptionKey(currentKey)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        // Generate new credentials
        subscription.setSubscriptionKey(generateRandomKey());
        subscription.setSubscriptionSecret(generateRandomSecret());

        Subscription updated = repository.save(subscription);

        log.info("Credentials regenerated successfully");
        return SubscriptionMapper.toResponse(updated);
    }

    /**
     * Gets only the API credentials for a subscription (without full details)
     *
     * @param subscriptionId the ID of the subscription
     * @return ApiCredentialsResponse containing only key and secret
     */
    public ApiCredentialsResponse getCredentials(Long subscriptionId) {
        log.debug("Fetching credentials for subscription ID: {}", subscriptionId);

        Subscription subscription = repository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        return new ApiCredentialsResponse(
                subscription.getSubscriptionKey(),
                subscription.getSubscriptionSecret()
        );
    }


    private String generateRandomKey() {
        // generate an alphanumeric string of length 24
        return  randomString(24);
    }

    private String generateRandomSecret() {
        return randomString(48);
    }

    private String randomString(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
