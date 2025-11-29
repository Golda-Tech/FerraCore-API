package com.goldatech.paymentservice.web.dto.response.momo;

public record ApprovedPreapproval(
        String preApprovalId,
        String toFri,
        String fromFri,
        String fromCurrency,
        String createdTime,
        String approvedTime,
        String expiryTime,
        String status,
        String message,
        String frequency,
        String startDate,
        String lastUsedDate,
        String offer,
        String externalId,
        String maxDebitAmount
) {
}
