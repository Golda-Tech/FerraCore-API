package com.goldatech.authservice.domain.dto;   // <- DTO package, not model

import java.math.BigDecimal;


import lombok.Value;
import lombok.Builder;

@Value
@Builder
public class UserTransactionSummary {
    long totalTransactionCount;
    BigDecimal successfulTotalTransactionAmount;
    BigDecimal failedTotalTransactionAmount;
    long failedTransactionCount;
    long successTransactionCount;
}