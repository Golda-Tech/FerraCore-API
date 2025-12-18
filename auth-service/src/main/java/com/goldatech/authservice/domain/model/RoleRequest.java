package com.goldatech.authservice.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "role_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private String requestedRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleRequestStatus status;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime processedAt;
    private String processedBy;
    private String note;
}
