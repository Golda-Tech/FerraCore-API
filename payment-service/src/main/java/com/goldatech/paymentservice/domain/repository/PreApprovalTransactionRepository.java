package com.goldatech.paymentservice.domain.repository;

import com.goldatech.paymentservice.domain.model.PaymentTransaction;
import com.goldatech.paymentservice.domain.model.PreApprovalTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PreApprovalTransactionRepository extends JpaRepository<PreApprovalTransaction, Long> {

    Optional<PaymentTransaction> findByTransactionRef(String transactionRef);
    Optional<PaymentTransaction> findByExternalRef(String externalRef);
    List<PaymentTransaction> findByInitiatedAtBetween(LocalDateTime start, LocalDateTime end);

}
