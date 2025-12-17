package com.goldatech.authservice.domain.repository;

import com.goldatech.authservice.domain.model.Subscription;
import com.goldatech.authservice.domain.model.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    boolean existsByContactEmail(String email);
    boolean existsByOrganizationName(String organization);

    @Query("SELECT s FROM Subscription s WHERE s.contact_email <> :excludeId AND " +
           "(:numbers MEMBER OF s.whitelistedNumber1 OR " +
           ":numbers MEMBER OF s.whitelistedNumber2 OR " +
           ":numbers MEMBER OF s.whitelistedNumber3 OR " +
           ":numbers MEMBER OF s.whitelistedNumber4)")
    List<Subscription> findByAnyWhitelistedNumber(@Param("numbers") List<String> numbers,
                                                  @Param("excludeId") String excludeId);


}
