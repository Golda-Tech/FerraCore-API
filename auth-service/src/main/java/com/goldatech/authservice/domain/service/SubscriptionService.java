package com.goldatech.authservice.domain.service;

import com.goldatech.authservice.domain.mapper.SubscriptionMapper;
import com.goldatech.authservice.domain.model.Subscription;
import com.goldatech.authservice.domain.model.SubscriptionStatus;
import com.goldatech.authservice.domain.repository.SubscriptionRepository;
import com.goldatech.authservice.security.JwtService;
import com.goldatech.authservice.web.dto.request.SubscriptionCreateRequest;
import com.goldatech.authservice.web.dto.request.SubscriptionLoginRequest;
import com.goldatech.authservice.web.dto.request.SubscriptionUpdateRequest;
import com.goldatech.authservice.web.dto.response.AuthResponse;
import com.goldatech.authservice.web.dto.response.SubscriptionAuthResponse;
import com.goldatech.authservice.web.dto.response.SubscriptionResponse;
import lombok.extern.slf4j.Slf4j;
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


    public SubscriptionAuthResponse authenticate(SubscriptionLoginRequest request) {
        log.info("Authenticating subscription with key: {}", request.subscriptionKey());

        var subscription = repository.findBySubscriptionKey(request.subscriptionKey())
                .filter(sub -> sub.getSubscriptionSecret().equals(request.subscriptionSecret()))
                .orElseThrow(() -> new RuntimeException("Invalid subscription credentials"));

        var jwtToken = jwtService.generateToken(subscription);

        return new SubscriptionAuthResponse(
                jwtToken,
                subscription.getId(),
                subscription.getOrganizationName(),
                subscription.getPlanType(),
                subscription.getStatus(),
                subscription.getContactEmail(),
                "Subscription authenticated successfully."
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


    private String generateRandomKey() {
        return "sub_" + randomString(24);
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
