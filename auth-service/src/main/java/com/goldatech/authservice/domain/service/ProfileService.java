package com.goldatech.authservice.domain.service;

import com.goldatech.authservice.domain.model.Subscription;
import com.goldatech.authservice.domain.model.User;
import com.goldatech.authservice.domain.repository.SubscriptionRepository;
import com.goldatech.authservice.domain.repository.UserRepository;
import com.goldatech.authservice.web.dto.request.OrganizationUpdateRequest;
import com.goldatech.authservice.web.dto.request.ProfileUpdateRequest;
import com.goldatech.authservice.web.dto.response.UserProfileResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    /**
     * Retrieves the complete profile for a user including organization and subscription details
     *
     * @param email the email of the authenticated user
     * @return UserProfileResponse with complete profile data
     */
    public UserProfileResponse getUserProfile(String email) {
        log.info("Fetching profile for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Find subscription by contact email (assuming user email matches contact email)
        Subscription subscription = subscriptionRepository.findByContactEmail(email)
                .orElse(null);

        return buildProfileResponse(user, subscription);
    }

    /**
     * Updates user profile information
     *
     * @param email the email of the authenticated user
     * @param request the profile update request
     * @return updated UserProfileResponse
     */
    @Transactional
    public UserProfileResponse updateProfile(String email, ProfileUpdateRequest request) {
        log.info("Updating profile for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.firstName() != null) {
            user.setFirstname(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastname(request.lastName());
        }
        if (request.email() != null && !request.email().equals(user.getEmail())) {
            // Check if new email already exists
            if (userRepository.existsByEmail(request.email())) {
                throw new RuntimeException("Email already in use");
            }
            user.setEmail(request.email());
        }

        userRepository.save(user);

        Subscription subscription = subscriptionRepository.findByContactEmail(email)
                .orElse(null);

        // Update the contact email in subscription if user email changed
        if (subscription != null && request.email() != null && !request.email().equals(email)) {
            subscription.setContactEmail(request.email());
            subscriptionRepository.save(subscription);
        }

        return buildProfileResponse(user, subscription);
    }

    /**
     * Updates organization details
     *
     * @param email the email of the authenticated user
     * @param request the organization update request
     * @return updated UserProfileResponse
     */
    @Transactional
    public UserProfileResponse updateOrganization(String email, OrganizationUpdateRequest request) {
        log.info("Updating organization for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Subscription subscription = subscriptionRepository.findByContactEmail(email)
                .orElseThrow(() -> new RuntimeException("Subscription not found for user"));

        if (request.address() != null) {
            subscription.setContactAddress(request.address());
        }
        // Note: You'll need to add these fields to the Subscription entity
        // For now, they'll be stored in the contact fields or you need to extend the entity

        subscriptionRepository.save(subscription);

        return buildProfileResponse(user, subscription);
    }

    /**
     * Regenerates API credentials for a subscription
     *
     * @param email the email of the authenticated user
     * @return updated UserProfileResponse with new credentials
     */
    @Transactional
    public UserProfileResponse regenerateApiCredentials(String email) {
        log.info("Regenerating API credentials for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Subscription subscription = subscriptionRepository.findByContactEmail(email)
                .orElseThrow(() -> new RuntimeException("Subscription not found for user"));

        // Generate new credentials
        subscription.setSubscriptionKey(generateRandomKey());
        subscription.setSubscriptionSecret(generateRandomSecret());

        subscriptionRepository.save(subscription);

        log.warn("API credentials regenerated for user: {}", email);

        return buildProfileResponse(user, subscription);
    }

    /**
     * Builds a complete profile response from user and subscription data
     */
    private UserProfileResponse buildProfileResponse(User user, Subscription subscription) {
        UserProfileResponse.OrganizationDetails orgDetails = null;
        UserProfileResponse.SubscriptionDetails subDetails = null;
        UserProfileResponse.ApiCredentials apiCreds = null;

        if (subscription != null) {
            orgDetails = UserProfileResponse.OrganizationDetails.builder()
                    .name(subscription.getOrganizationName())
                    .businessType("Technology") // Default or fetch from extended entity
                    .address(subscription.getContactAddress())
                    .registrationNumber("") // Add to entity if needed
                    .taxId("") // Add to entity if needed
                    .website("") // Add to entity if needed
                    .build();

            subDetails = UserProfileResponse.SubscriptionDetails.builder()
                    .plan(subscription.getPlanType().displayName().toUpperCase())
                    .status(subscription.getStatus())
                    .billingCycle("monthly") // Default or add to entity
                    .nextBilling(calculateNextBilling(subscription.getCreatedAt()))
                    .amount(getPlanAmount(subscription.getPlanType()))
                    .currency("GHS")
                    .build();

            apiCreds = UserProfileResponse.ApiCredentials.builder()
                    .subscriptionKey(subscription.getSubscriptionKey())
                    .subscriptionSecret(subscription.getSubscriptionSecret())
                    .build();
        }

        return UserProfileResponse.builder()
                .id(user.getId().toString())
                .firstName(user.getFirstname())
                .lastName(user.getLastname())
                .email(user.getEmail())
                .isFirstTimeUser(user.isFirstTimeUser())
                .phone("") // Add phone field to User entity if needed
                .role(user.getRole())
                .organization(orgDetails)
                .subscription(subDetails)
                .apiCredentials(apiCreds)
                .build();
    }

    /**
     * Calculates the next billing date (30 days from creation or last billing)
     */
    private LocalDateTime calculateNextBilling(LocalDateTime createdAt) {
        if (createdAt == null) {
            return LocalDateTime.now().plusDays(30);
        }
        // Simple calculation - in production, use proper billing cycle logic
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextBilling = createdAt.plusMonths(1);

        while (nextBilling.isBefore(now)) {
            nextBilling = nextBilling.plusMonths(1);
        }

        return nextBilling;
    }

    /**
     * Gets the plan amount based on plan type
     */
    private Double getPlanAmount(com.goldatech.authservice.domain.model.PlanType planType) {
        return switch (planType) {
            case PAYMENT_REQUEST -> 199.0;
            case PAYOUTS -> 249.0;
            case RECURRING_PAYMENTS-> 179.0;
            case ENTERPRISE_FULL_ACCESS -> 499.0;
        };
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
