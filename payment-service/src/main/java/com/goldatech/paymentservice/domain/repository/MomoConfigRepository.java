package com.goldatech.paymentservice.domain.repository;


import java.util.Optional;

import com.goldatech.paymentservice.domain.model.TelcoProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import com.goldatech.paymentservice.domain.model.MomoConfigEntity;

public interface MomoConfigRepository extends JpaRepository<MomoConfigEntity, Long> {
    Optional<MomoConfigEntity> findTopByProviderOrderByIdAsc(TelcoProvider provider);
}
