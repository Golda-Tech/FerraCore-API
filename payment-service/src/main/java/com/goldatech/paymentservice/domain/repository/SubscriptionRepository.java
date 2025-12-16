package com.goldatech.paymentservice.domain.repository;

import com.goldatech.paymentservice.domain.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SubscriptionRepository  extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByWhitelistedNumber1(String mobileNumber);
    Optional<Subscription> findByWhitelistedNumber2(String mobileNumber);
    Optional<Subscription> findByWhitelistedNumber3(String mobileNumber);

    @Query("SELECT s FROM Subscription s " +
           "WHERE :number IN (s.whitelistedNumber1, s.whitelistedNumber2, s.whitelistedNumber3, s.whitelistedNumber4)")
    Optional<Subscription> findByWhitelistedNumbers(@Param("number") String number);
}
