package com.goldatech.authservice.domain.repository;

import com.goldatech.authservice.domain.model.PaymentTransaction;
import com.goldatech.authservice.domain.model.UserTransactionSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;



@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    @Query("""
           SELECT new com.goldatech.authservice.domain.dto.UserTransactionSummary(
                  COUNT(pt),
                  COALESCE(SUM(pt.amount), 0)
           )
           FROM PaymentTransaction pt
           WHERE pt.initiatedBy = :userEmail
             AND pt.status = 'SUCCESSFUL'
           """)
    UserTransactionSummary getUserTransactionSummary(@Param("userEmail") String userEmail);
}