package com.goldatech.authservice.domain.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class UserTransactionSummary {
    long   totalTransactionCount;
    BigDecimal successfulTotalTransactionAmount;
}