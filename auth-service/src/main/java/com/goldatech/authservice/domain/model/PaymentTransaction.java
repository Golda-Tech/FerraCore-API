package com.goldatech.authservice.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String transactionRef; // Internal reference
    private String externalRef;    // Third-party reference
    private String collectionRef;  // Reference from the collections service
    private String provider;       // "MTN", "VODAFONE", etc.
    private String mobileNumber;
    private BigDecimal amount;
    private String currency;
    private String initiatedBy;    // User ID or system that initiated the transaction
    private String initiationPartner;    // Partner through which the transaction was initiated
    private Role userRoles;    // Access level of the initiator
    private TransactionStatus status;
    private String message;
    private LocalDateTime initiatedAt;
    private LocalDateTime completedAt;
    private String mtnFinancialTransactionId; // MTN specific field
    private String mtnExternalId; // MTN specific field
    private String mtnPayerPartyIdType; // MTN specific field
    private String mtnPayerPartyId; // MTN specific field
    private String mtnPayerMessage; // MTN specific field
    private String mtnPayeeNote; // MTN specific field

}
