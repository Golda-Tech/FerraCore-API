package com.goldatech.authservice.domain.service;

import com.goldatech.authservice.domain.model.*;
import com.goldatech.authservice.domain.repository.PartnerSummaryRepository;
import com.goldatech.authservice.domain.repository.PaymentTransactionRepository;
import com.goldatech.authservice.domain.repository.SubscriptionRepository;
import com.goldatech.authservice.domain.repository.UserRepository;
import com.goldatech.authservice.web.dto.request.OrganizationUpdateRequest;
import com.goldatech.authservice.web.dto.request.ProfileUpdateRequest;
import com.goldatech.authservice.web.dto.request.WhitelistUpdateRequest;
import com.goldatech.authservice.web.dto.response.UserProfileResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PartnerSummaryRepository partnerSummaryRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

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

        if (subscription != null) {
            subscription.setBusinessType("Technology"); // Temporary hardcoded value
        }
        subscriptionRepository.save(Objects.requireNonNull(subscription));

        return buildProfileResponse(user, subscription);
    }

    public List<UserProfileResponse> getUsers(String orgName) {
        log.info("Fetching all users for organization: {}", orgName);

        /* 1. all subscriptions for this organisation (any status) */
        List<Subscription> subs = subscriptionRepository.findByOrganizationNameIgnoreCase(orgName);

        /* 2. map each subscription to its user + live stats */
        return subs.stream()
                .map(sub -> {
                    User user = userRepository.findByEmail(sub.getContactEmail())
                            .orElseThrow(() -> new RuntimeException(
                                    "No user for e-mail " + sub.getContactEmail()));

                    /* live stats from payment_transactions */
                    UserTransactionSummary summary =
                            paymentTransactionRepository.getUserTransactionSummary(user.getEmail());

                    return buildProfileResponse(user, sub, summary);
                })
                .toList();
    }


    public List<UserProfileResponse> getPartners() {
        return subscriptionRepository.findAll()
                .stream()
                .collect(Collectors.toMap(Subscription::getOrganizationName,
                        Function.identity(),
                        (s1, s2) -> s1))          // distinct by org name
                .values()
                .stream()
                .map(this::buildResponseForPartner)
                .toList();
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

        if (request.phone() != null) {
            user.setPhoneNumber(request.phone());
        }
        if (request.email() != null && !request.email().equals(user.getEmail())) {
            // Check if new email already exists
            if (userRepository.existsByEmail(request.email())) {
                throw new RuntimeException("Email already in use");
            }
            user.setEmail(request.email());
        }
        user.setFirstTimeUser(false);

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
        if (request.website() != null) {
            subscription.setWebsite(request.website());
        }
        if (request.registrationNumber() != null) {
            subscription.setRegistrationNumber(request.registrationNumber());
        }
        if (request.taxId() != null) {
            subscription.setTaxId(request.taxId());
        }

        // Note: You'll need to add these fields to the Subscription entity
        // For now, they'll be stored in the contact fields or you need to extend the entity

        subscriptionRepository.save(subscription);
        user.setFirstTimeUser(false);
        userRepository.save(user);

        return buildProfileResponse(user, subscription);
    }

    @Transactional
    public UserProfileResponse updateCallbackUrl(String email, String callbackUrl) {
        log.info("Updating callback url for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Subscription subscription = subscriptionRepository.findByContactEmail(email)
                .orElseThrow(() -> new RuntimeException("Subscription not found for user"));

        if (callbackUrl != null) {
            subscription.setCallbackUrl(callbackUrl);
        }
        subscriptionRepository.save(subscription);
        user.setFirstTimeUser(false);
        userRepository.save(user);

        return buildProfileResponse(user, subscription);
    }

    @Transactional
    public UserProfileResponse updateWhitelistedIds(String email, WhitelistUpdateRequest request) {
        String phone1 = request.phone1();
        String phone2 = request.phone2();
        String phone3 = request.phone3();
        String phone4 = request.phone4();

        log.info("Updating whitelisted phone numbers for user: {}", email);
        log.info("New whitelisted numbers: {}, {}, {}, {}", phone1, phone2, phone3, phone4);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Subscription subscription = subscriptionRepository.findByContactEmail(email)
                .orElseThrow(() -> new RuntimeException("Subscription not found for user"));

        List<String> newPhones = Stream.of(phone1, phone2, phone3, phone4)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        /* ignore rows that belong to THIS subscription */
        List<Subscription> conflicts = subscriptionRepository
                .findByAnyWhitelistedNumber(newPhones, subscription.getId());

        if (!conflicts.isEmpty()) {
            /* build a clean list of the *first* offending number(s) */
            Set<String> incoming = Set.copyOf(newPhones);
            String dup = conflicts.stream()
                    .flatMap(s -> Stream.of(s.getWhitelistedNumber1(),
                            s.getWhitelistedNumber2(),
                            s.getWhitelistedNumber3(),
                            s.getWhitelistedNumber4()))
                    .filter(incoming::contains)
                    .findFirst()                       // one is enough for the message
                    .orElse("");

            throw new RuntimeException("Phone number " + dup + " is already in use by another user.");
        }

        subscription.setWhitelistedNumber1(phone1);
        subscription.setWhitelistedNumber2(phone2);
        subscription.setWhitelistedNumber3(phone3);
        subscription.setWhitelistedNumber4(phone4);

        subscriptionRepository.save(subscription);
        user.setFirstTimeUser(false);
        userRepository.save(user);

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
                    .businessType(subscription.getBusinessType())
                    .address(subscription.getContactAddress())
                    .registrationNumber(subscription.getRegistrationNumber())
                    .taxId(subscription.getTaxId())
                    .website(subscription.getWebsite())
                    .build();

            subDetails = UserProfileResponse.SubscriptionDetails.builder()
                    .plan(subscription.getPlanType().displayName().toUpperCase())
                    .status(subscription.getStatus())
                    .billingCycle("monthly")
                    .nextBilling(calculateNextBilling(subscription.getCreatedAt()))
                    .callbackUrl(subscription.getCallbackUrl())
                    .whitelistedNumber1(subscription.getWhitelistedNumber1())
                    .whitelistedNumber2(subscription.getWhitelistedNumber2())
                    .whitelistedNumber3(subscription.getWhitelistedNumber3())
                    .whitelistedNumber4(subscription.getWhitelistedNumber4())
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
                .phone(user.getPhoneNumber())
                .role(user.getRole())
                .organization(orgDetails)
                .subscription(subDetails)
                .apiCredentials(apiCreds)
                .build();
    }

    private UserProfileResponse buildResponseForPartner(Subscription sub) {
        User user = userRepository.findByEmail(sub.getContactEmail())
                .orElseThrow(() -> new RuntimeException("User not found: " + sub.getContactEmail()));

        PartnerSummary ps = partnerSummaryRepository
                .findByPartnerId(user.getEmail())
                .orElse(null);

        /* build base response */
        UserProfileResponse.UserProfileResponseBuilder builder =
                UserProfileResponse.builder()
                        .id(user.getId().toString())
                        .firstName(user.getFirstname())
                        .lastName(user.getLastname())
                        .email(user.getEmail())
                        .isFirstTimeUser(user.isFirstTimeUser())
                        .phone(user.getPhoneNumber())
                        .role(user.getRole())
                        .organization(buildOrgDetails(sub))
                        .subscription(buildSubDetails(sub))
                        .apiCredentials(buildApiCreds(sub));


        if (ps != null) {
            builder.summary(UserProfileResponse.Summary.builder()
                    .partnerId(ps.getPartnerId())
                    .partnerName(ps.getPartnerName())
                    .totalAmountTransactions(new BigDecimal(String.valueOf(ps.getTotalAmountTransactions())).doubleValue())
                    .totalCountTransactions(ps.getTotalCountTransactions())
                    .build());
        }

        return builder.build();
    }

    private UserProfileResponse buildProfileResponse(User u,
                                                     Subscription s,
                                                     UserTransactionSummary uts) {

        /* ordinary sub-objects first */
        UserProfileResponse.OrganizationDetails org = buildOrgDetails(s);
        UserProfileResponse.SubscriptionDetails sub = buildSubDetails(s);
        UserProfileResponse.ApiCredentials api = buildApiCreds(s);

        /* summary node */
        UserProfileResponse.Summary summary = UserProfileResponse.Summary.builder()
                .partnerId(u.getEmail())
                .partnerName(u.getFirstname() + " " + u.getLastname())
                .totalCountTransactions(String.valueOf(uts.getTotalTransactionCount()))
                .totalAmountTransactions(uts.getSuccessfulTotalTransactionAmount().doubleValue())
                .build();

        /* final immutable object */
        return UserProfileResponse.builder()
                .id(u.getId().toString())
                .firstName(u.getFirstname())
                .lastName(u.getLastname())
                .email(u.getEmail())
                .isFirstTimeUser(u.isFirstTimeUser())
                .phone(u.getPhoneNumber())
                .role(u.getRole())
                .organization(org)
                .subscription(sub)
                .apiCredentials(api)
                .summary(summary)
                .build();
    }


    private UserProfileResponse.OrganizationDetails buildOrgDetails(Subscription s) {
        return UserProfileResponse.OrganizationDetails.builder()
                .name(s.getOrganizationName())
                .businessType(s.getBusinessType())
                .address(s.getContactAddress())
                .registrationNumber(s.getRegistrationNumber())
                .taxId(s.getTaxId())
                .website(s.getWebsite())
                .build();
    }

    private UserProfileResponse.SubscriptionDetails buildSubDetails(Subscription s) {
        return UserProfileResponse.SubscriptionDetails.builder()
                .plan(s.getPlanType().displayName().toUpperCase())
                .status(s.getStatus())
                .billingCycle("monthly")
                .nextBilling(calculateNextBilling(s.getCreatedAt()))
                .callbackUrl(s.getCallbackUrl())
                .whitelistedNumber1(s.getWhitelistedNumber1())
                .whitelistedNumber2(s.getWhitelistedNumber2())
                .whitelistedNumber3(s.getWhitelistedNumber3())
                .whitelistedNumber4(s.getWhitelistedNumber4())
                .amount(getPlanAmount(s.getPlanType()))
                .currency("GHS")
                .build();
    }

    private UserProfileResponse.ApiCredentials buildApiCreds(Subscription s) {
        return UserProfileResponse.ApiCredentials.builder()
                .subscriptionKey(s.getSubscriptionKey())
                .subscriptionSecret(s.getSubscriptionSecret())
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
    private Double getPlanAmount(PlanType planType) {
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
