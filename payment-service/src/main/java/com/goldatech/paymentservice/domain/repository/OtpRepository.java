package com.goldatech.paymentservice.domain.repository;

import com.goldatech.paymentservice.domain.model.Otp;
import com.goldatech.paymentservice.domain.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {
    Optional<Otp> findTopByUserIdAndTypeOrderByCreatedAtDesc(String userId, String type);

    Optional<Otp> findTopByEmailAndTypeOrderByCreatedAtDesc(String email, String type);

    Optional<Otp> findTopByMobileNumberAndTypeOrderByCreatedAtDesc(String mobileNumber, String type);

    void deleteByExpiresAtBefore(java.time.LocalDateTime dateTime);


    Optional<Otp> findTopByMobileNumberAndOtpCodeOrderByCreatedAtDesc(String mobileNumber, String otp);

    Optional<Otp> findTopByEmailAndOtpCodeOrderByCreatedAtDesc(String email, String otp);
}
