package com.goldatech.paymentservice.domain.repository;

import com.goldatech.paymentservice.domain.model.MtnCallback;
import com.goldatech.paymentservice.domain.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MtnCallbackRepository extends JpaRepository<MtnCallback, Long> {

    //Find by externalId
    Optional<MtnCallback> findByExternalId(String externalId);
    //Find by financialTransactionId
    Optional<MtnCallback> findByFinancialTransactionId(String financialTransactionId);
}
