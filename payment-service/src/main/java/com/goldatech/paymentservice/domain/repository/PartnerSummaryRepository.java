package com.goldatech.paymentservice.domain.repository;

import com.goldatech.paymentservice.domain.model.PartnerSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface PartnerSummaryRepository extends JpaRepository<PartnerSummary, Long> {

    @Query(value = "SELECT partner_id FROM partner_summary WHERE LOWER(partner_name) = LOWER(:name) LIMIT 1", nativeQuery = true)
    Optional<String> findPartnerIdByNameIgnoreCase(@Param("name") String partnerName);


    @Modifying
    @Query(value = """
        INSERT INTO partner_summary(partner_id, partner_name,
                                    total_amount_transactions, total_count_transactions)
        VALUES (:pid, :name, :amt, 1)
        ON CONFLICT (partner_id)
        DO UPDATE
            SET total_amount_transactions = (partner_summary.total_amount_transactions)::NUMERIC + :amt,
                total_count_transactions    = (partner_summary.total_count_transactions)::INTEGER + 1
        """, nativeQuery = true)
    void upsertPartnerSummary(@Param("pid") String partnerId,
                              @Param("name") String partnerName,
                              @Param("amt") BigDecimal amount);
}
