package com.goldatech.authservice.domain.dto;

import java.math.BigDecimal;

public record TransactionSummaryDTO(
        Long totalTransactions,           // Count of all transactions
        Long successfulTransactions,      // Count of successful transactions
        BigDecimal successfulAmount,      // Amount of successful transactions (duplicate of totalAmount)
        Long failedTransactions,          // Count of failed transactions
        BigDecimal failedAmount           // Amount of failed transactions
) {}