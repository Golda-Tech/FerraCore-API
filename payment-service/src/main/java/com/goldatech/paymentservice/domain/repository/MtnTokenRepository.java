package com.goldatech.paymentservice.domain.repository;

import com.goldatech.paymentservice.domain.model.momo.MtnToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MtnTokenRepository extends JpaRepository<MtnToken, Long> {
    Optional<MtnToken> findTopByTypeOrderByCreatedAtDesc(String type);
}
