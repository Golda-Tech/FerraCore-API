package com.goldatech.authservice.domain.repository;

import com.goldatech.authservice.domain.dto.TransactionSummaryDTO;
import com.goldatech.authservice.domain.model.PaymentTransaction;
import com.goldatech.authservice.domain.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Map;


@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

//    @Query("SELECT COUNT(pt) AS totalTransactionCount, " +
//           "COALESCE(SUM(pt.amount), 0) AS successfulTotalTransactionAmount " +
//           "FROM PaymentTransaction pt " +
//           "WHERE pt.initiatedBy = :initiatedBy AND pt.status = 'SUCCESSFUL'")
//    Map<String, Object> getUserTransactionSummary(@Param("initiatedBy") String initiatedBy);

    @Query("""
    SELECT new com.goldatech.authservice.domain.dto.TransactionSummaryDTO(
        COUNT(pt),
        COALESCE(SUM(CASE WHEN pt.status = :success THEN pt.amount ELSE 0 END), 0),
        SUM(CASE WHEN pt.status = :success THEN 1L ELSE 0L END),
        COALESCE(SUM(CASE WHEN pt.status = :success THEN pt.amount ELSE 0 END), 0),
        SUM(CASE WHEN pt.status = :failed THEN 1L ELSE 0L END),
        COALESCE(SUM(CASE WHEN pt.status = :failed THEN pt.amount ELSE 0 END), 0)
    )
    FROM PaymentTransaction pt
    WHERE pt.initiatedBy = :initiatedBy
""")
    TransactionSummaryDTO getUserTransactionSummary(
            @Param("initiatedBy") String initiatedBy,
            @Param("successful") TransactionStatus successful,
            @Param("failed") TransactionStatus failed
    );
}