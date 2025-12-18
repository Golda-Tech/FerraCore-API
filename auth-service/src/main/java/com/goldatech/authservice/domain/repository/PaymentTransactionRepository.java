package com.goldatech.authservice.domain.repository;

import com.goldatech.authservice.domain.model.PaymentTransaction;
import com.goldatech.authservice.domain.dto.UserTransactionSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Map;


@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    @Query("SELECT COUNT(pt) AS totalTransactionCount, " +
           "COALESCE(SUM(pt.amount), 0) AS successfulTotalTransactionAmount " +
           "FROM PaymentTransaction pt " +
           "WHERE pt.initiatedBy = :userEmail AND pt.status = 'SUCCESSFUL'")
    Map<String, Object> getUserTransactionSummary(@Param("userEmail") String userEmail);
}