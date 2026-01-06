package com.goldatech.paymentservice.web.dto.response.momo;

import com.goldatech.paymentservice.web.dto.request.momo.Payer;

import java.time.Instant;

public record PreApprovalStatusResponse(
        Payer payer,
        String payerCurrency,
        String payerMessage,
        String status,
        Instant expirationDateTime,
        ErrorReason reason
) {
}
