package com.goldatech.paymentservice.domain.repository;

import com.goldatech.paymentservice.domain.model.PartnerSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PartnerSummaryRepository extends JpaRepository<PartnerSummary, Long> {

    @Query(value = "SELECT partner_id FROM partner_summary WHERE LOWER(partner_name) = LOWER(:name) LIMIT 1", nativeQuery = true)
    Optional<String> findPartnerIdByNameIgnoreCase(@Param("name") String partnerName);
}
