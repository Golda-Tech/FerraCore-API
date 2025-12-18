package com.goldatech.authservice.domain.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionSummaryDTO {
    // Add this as the first field to match the SELECT order
    private String email;

    private Long totalTransactions;
    private Double totalSuccessAmount;
    private Long totalSuccessCount;
    private Long totalFailedCount;
    private Double totalFailedAmount;
}