package com.goldatech.paymentservice.domain.repository;

import com.goldatech.paymentservice.domain.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository  extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByWhitelistedNumber1(String mobileNumber);
    Optional<Subscription> findByWhitelistedNumber2(String mobileNumber);
    Optional<Subscription> findByWhitelistedNumber3(String mobileNumber);
}
