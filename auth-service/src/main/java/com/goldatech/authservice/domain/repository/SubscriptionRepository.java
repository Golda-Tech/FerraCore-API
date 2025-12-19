package com.goldatech.authservice.domain.repository;

import com.goldatech.authservice.domain.model.Subscription;
import com.goldatech.authservice.domain.model.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
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

    @Query("SELECT s FROM Subscription s WHERE s.id <> :excludeId AND " +
           "(s.whitelistedNumber1 IN :numbers OR " +
           " s.whitelistedNumber2 IN :numbers OR " +
           " s.whitelistedNumber3 IN :numbers OR " +
           " s.whitelistedNumber4 IN :numbers)")
    List<Subscription> findByAnyWhitelistedNumber(@Param("numbers") Collection<String> numbers,
                                                  @Param("excludeId") Long excludeId);

    Optional<Subscription> findTopByOrganizationNameIgnoreCase(String orgName);

    List<Subscription> findByOrganizationNameIgnoreCase(String orgName);

    @Query(value =
            "SELECT organization_id FROM subscription " +
            "WHERE LOWER(organization_name) = LOWER(:orgName) " +
            "LIMIT 1", nativeQuery = true)
    Optional<String> findOrganizationIdByOrganizationName(@Param("orgName") String orgName);
}
