package com.goldatech.paymentservice.domain.repository;

import com.goldatech.paymentservice.domain.model.PartnerSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PartnerSummaryRepository extends JpaRepository<PartnerSummary, Long> {
    /* returns partner-id that matches the given partner-name */
    @Query(value = "SELECT partner_id FROM partner_summary WHERE partner_name = :name LIMIT 1", nativeQuery = true)
    Optional<String> findPartnerIdByName(@Param("name") String partnerName);

}
