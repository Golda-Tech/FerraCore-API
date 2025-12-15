package com.goldatech.paymentservice.domain.repository;

import com.goldatech.paymentservice.domain.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    List<PaymentTransaction> findByInitiatedBy(String initiatedBy);
    Optional<PaymentTransaction> findByTransactionRef(String transactionRef);
    Optional<PaymentTransaction> findByExternalRef(String externalRef);
    List<PaymentTransaction> findByInitiatedAtBetween(LocalDateTime start, LocalDateTime end);
    List<PaymentTransaction> findByInitiatedAtBetweenAndInitiatedBy(
            LocalDateTime start,
            LocalDateTime end,
            String initiatedBy
    );

    //Find all
}