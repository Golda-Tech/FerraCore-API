package com.goldatech.paymentservice.domain.model.momo;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "mtn_token", uniqueConstraints = {
        @UniqueConstraint(columnNames = "type")
})
public class MtnToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String type; // "COLLECTION" or "DISBURSEMENT"

    @Column(length = 2048)
    private String accessToken;

    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;
}
