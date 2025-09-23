package com.goldatech.authservice.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Otp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String mobileNumber;
    private String email;
    private String otpCode;
    private String userId;
    private String type; // e.g., "VERIFICATION", "RESET_PASSWORD"
    private String message;
    private String channel; // e.g., "SMS", "EMAIL"
    private String subject; // For email subject
    private boolean used; // To track if the OTP has been used
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;


    //Automatically delete OTP after 5 minutes
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        expiresAt = createdAt.plusMinutes(5);

    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }


}