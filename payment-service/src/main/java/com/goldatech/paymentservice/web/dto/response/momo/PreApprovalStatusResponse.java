package com.goldatech.paymentservice.web.dto.response.momo;

import com.goldatech.paymentservice.web.dto.request.momo.Payer;

public record PreApprovalStatusResponse(
        Payer payer,
        String payerCurrency,
        String payerMessage,
        String status,
        String expirationDateTime,
        ErrorReason reason
) {
}
