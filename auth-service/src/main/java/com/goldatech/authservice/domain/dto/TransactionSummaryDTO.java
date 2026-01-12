package com.goldatech.authservice.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal; // Import this

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionSummaryDTO {
    private String email;
    private Integer totalTransactions;
    private BigDecimal totalSuccessAmount;
    private Long totalSuccessCount;
    private Long totalFailedCount;
    private BigDecimal totalFailedAmount;
}