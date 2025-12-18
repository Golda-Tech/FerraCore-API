package com.goldatech.authservice.domain.repository;

import com.goldatech.authservice.domain.dto.TransactionSummaryDTO;
import com.goldatech.authservice.domain.model.PaymentTransaction;
import com.goldatech.authservice.domain.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;


@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    @Query("""
    SELECT new com.goldatech.authservice.domain.dto.TransactionSummaryDTO(
        pt.initiatedBy,
        COUNT(pt),
        COALESCE(SUM(CASE WHEN pt.status = :success THEN pt.amount ELSE 0.0 END), 0.0),
        SUM(CASE WHEN pt.status = :success THEN 1L ELSE 0L END),
        SUM(CASE WHEN pt.status = :failed THEN 1L ELSE 0L END),
        COALESCE(SUM(CASE WHEN pt.status = :failed THEN pt.amount ELSE 0.0 END), 0.0)
    )
    FROM PaymentTransaction pt
    WHERE pt.initiatedBy IN :emails
    GROUP BY pt.initiatedBy
""")
    List<TransactionSummaryDTO> getBulkUserTransactionSummaries(
            @Param("emails") List<String> emails,
            @Param("success") TransactionStatus success,
            @Param("failed") TransactionStatus failed
    );
}