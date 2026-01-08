package com.goldatech.paymentservice.web.dto.response.momo;

import com.goldatech.paymentservice.web.dto.request.momo.Payer;

import java.time.LocalDateTime;

public record PreApprovalStatusResponse(
        Payer payer,
        String payerCurrency,
        String payerMessage,
        String status,
        LocalDateTime expirationDateTime,
        Reason reason
) {
}
