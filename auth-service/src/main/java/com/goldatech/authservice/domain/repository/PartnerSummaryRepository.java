package com.goldatech.authservice.domain.repository;

import com.goldatech.authservice.domain.model.PartnerSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PartnerSummaryRepository  extends JpaRepository<PartnerSummary, Long> {
    @Query("SELECT ps FROM PartnerSummary ps WHERE ps.partnerName = :orgName")
    Optional<PartnerSummary> findByPartnerId(@Param("orgName") String orgName);
}
