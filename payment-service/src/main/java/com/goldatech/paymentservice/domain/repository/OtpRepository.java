package com.goldatech.paymentservice.domain.repository;

import com.goldatech.paymentservice.domain.model.Otp;
import com.goldatech.paymentservice.domain.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {
    Optional<Otp> findTopByUserIdAndTypeOrderByCreatedAtDesc(String userId, String type);

    Optional<Otp> findTopByEmailAndTypeOrderByCreatedAtDesc(String email, String type);

    Optional<Otp> findTopByMobileNumberAndTypeOrderByCreatedAtDesc(String mobileNumber, String type);

    void deleteByExpiresAtBefore(java.time.LocalDateTime dateTime);


    Optional<Otp> findTopByMobileNumberAndOtpCodeOrderByCreatedAtDesc(String mobileNumber, String otp);

    Optional<Otp> findTopByEmailAndOtpCodeOrderByCreatedAtDesc(String email, String otp);

    Optional<Otp> findTopByMobileNumberAndOtpCodeAndExpiresAtAfterAndUsedFalseOrderByCreatedAtDesc(
            String mobileNumber, String otp, LocalDateTime now
    );

    Optional<Otp> findTopByEmailAndOtpCodeAndExpiresAtAfterAndUsedFalseOrderByCreatedAtDesc(
            String email, String otp, LocalDateTime now
    );

}
