package com.goldatech.collectionsservice.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "collections")
public class Collection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Internal unique reference for this collection
    @Column(nullable = false, unique = true)
    private String collectionRef;

    // External reference ID from the payment gateway (if available)
    @Column(unique = true)
    private String externalRef;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CollectionStatus status;

    @Column(nullable = false)
    private LocalDateTime initiatedAt;

    private LocalDateTime updatedAt;
    private String description;
    private String failureReason;
}