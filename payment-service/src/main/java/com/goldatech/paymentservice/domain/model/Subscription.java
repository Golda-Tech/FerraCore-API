package com.goldatech.paymentservice.domain.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_name", nullable = false)
    private String organizationName;

    @Column(name = "subscription_key", unique = true)
    private String subscriptionKey;

    @Column(name = "subscription_secret", unique = true)
    private String subscriptionSecret;

    @Column(name = "plan_type")
    private PlanType planType;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_address")
    private String contactAddress;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "status")
    private SubscriptionStatus status;

    @Column(name = "business_type")
    private String businessType;

    @Column(name = "registration_number")
    private String registrationNumber;

    @Column(name = "whitelisted_number_1")
    private String whitelistedNumber1;

    @Column(name = "whitelisted_number_2")
    private String whitelistedNumber2;

    @Column(name = "whitelisted_number_3")
    private String whitelistedNumber3;

    @Column(name = "whitelisted_number_4")
    private String whitelistedNumber4;

    @Column(name = "tax_id")
    private String taxId;

    @Column(name = "website")
    private String website;

    @Column(name = "call_back_url")
    private String callbackUrl;

    @Column(name = "billing_cycle")
    private String billingCycle = "monthly";

    @Column(name = "subscription_amount")
    private Double subscriptionAmount;

    @Column(name = "currency")
    private String currency = "GHS";

    @Column(name = "next_billing_date")
    private LocalDateTime nextBillingDate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

