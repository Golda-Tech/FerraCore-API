package com.goldatech.paymentservice.domain.model.momo;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MtnToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type; // "COLLECTION" or "DISBURSEMENT"
    private String accessToken;

    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;
}
