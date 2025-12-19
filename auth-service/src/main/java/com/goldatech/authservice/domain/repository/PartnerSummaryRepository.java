package com.goldatech.authservice.domain.repository;

import com.goldatech.authservice.domain.model.PartnerSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PartnerSummaryRepository  extends JpaRepository<PartnerSummary, Long> {

    @Query("SELECT ps FROM PartnerSummary ps WHERE LOWER(ps.partnerName) = LOWER(:orgName)")
    Optional<PartnerSummary> findByPartnerNameIgnoreCase(@Param("orgName") String orgName);

    // Find by partnerId directly
    Optional<PartnerSummary> findByPartnerId(String partnerId);

    // Check if organization exists
    boolean existsByPartnerName(String partnerName);
}
