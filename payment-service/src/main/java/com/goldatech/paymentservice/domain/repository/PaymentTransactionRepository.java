package com.goldatech.paymentservice.domain.repository;

import com.goldatech.paymentservice.domain.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByTransactionRef(String transactionRef);
    Optional<PaymentTransaction> findByExternalRef(String externalRef);
}