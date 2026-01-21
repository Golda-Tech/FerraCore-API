package com.goldatech.paymentservice.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_ledger")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentLedger {

    @Id
    @Column(name = "transaction_id", nullable = false, columnDefinition = "UUID")
    private UUID transactionId;

    @Column(name = "partner_name", nullable = false)
    private String partnerName;

    @Column(name = "trans_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal transAmount;

    @Column(name = "gateway_fee", nullable = false, precision = 19, scale = 4)
    private BigDecimal gatewayFee;

    @Column(name = "billing_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal billingAmount;

    @Column(name = "provider_fee", nullable = false, precision = 19, scale = 4)
    private BigDecimal providerFee;

    @Column(name = "settle_0_mtn", nullable = false, precision = 19, scale = 2)
    private BigDecimal settle0Mtn;

    @Column(name = "settle_1_partner", nullable = false, precision = 19, scale = 2)
    private BigDecimal settle1Partner;

    @Column(name = "margin_earned",
            precision = 19,
            scale = 2,
            insertable = false,
            updatable = false,
            columnDefinition = "NUMERIC(19,2) GENERATED ALWAYS AS (settle_0_mtn - settle_1_partner) STORED")
    private BigDecimal marginEarned;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP", updatable = false)
    private OffsetDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;
}
