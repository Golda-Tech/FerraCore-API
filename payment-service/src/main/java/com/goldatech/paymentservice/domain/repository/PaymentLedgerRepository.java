package com.goldatech.paymentservice.domain.repository;

import com.goldatech.paymentservice.domain.model.PaymentLedger;
import com.goldatech.paymentservice.domain.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PaymentLedgerRepository extends JpaRepository<PaymentLedger, UUID> {

    @Modifying
    @Query("update PaymentLedger p set p.status = :status where p.transactionId = :transactionId")
    void updateStatusByTransactionId(@Param("status") TransactionStatus status,
                                     @Param("transactionId") UUID transactionId);
}
