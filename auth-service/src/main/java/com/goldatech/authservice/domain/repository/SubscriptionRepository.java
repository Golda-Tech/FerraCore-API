package com.goldatech.authservice.domain.repository;

import com.goldatech.authservice.domain.model.Subscription;
import com.goldatech.authservice.domain.model.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findBySubscriptionKey(String subscriptionKey);

    //Exists by subscription key
    boolean existsBySubscriptionKey(String subscriptionKey);

    //Find by organization name
    Optional<Subscription> findByOrganizationName(String organizationName);

    //Find all by status
    List<Subscription> findAllByStatus(SubscriptionStatus status);

    Optional<Subscription> findByContactEmail(String contactEmail);

    boolean existsByEmail(String email);
    boolean existsByOrganizationName(String organization);

}
