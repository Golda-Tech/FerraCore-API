package com.goldatech.paymentservice.web.dto.request;

import java.util.Map;

public record MtnCallBackRequest(
        String externalId,
        String amount,
        String currency,
        Map<String, String> payer,
        String payerMessage,
        String payeeNote,
        String status,
        String reason,
        String financialTransactionId
) {
}
