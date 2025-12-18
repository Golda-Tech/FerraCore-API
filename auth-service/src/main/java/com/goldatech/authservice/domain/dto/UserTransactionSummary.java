package com.goldatech.authservice.domain.dto;   // <- DTO package, not model

import lombok.Value;
import java.math.BigDecimal;

@Value   // no @Entity, no @Table, no @Id
public class UserTransactionSummary {
    long totalTransactionCount;
    BigDecimal successfulTotalTransactionAmount;

    /* constructor for JPQL */
    public UserTransactionSummary(long totalTransactionCount, BigDecimal successfulTotalTransactionAmount) {
        this.totalTransactionCount = totalTransactionCount;
        this.successfulTotalTransactionAmount = successfulTotalTransactionAmount;
    }
}