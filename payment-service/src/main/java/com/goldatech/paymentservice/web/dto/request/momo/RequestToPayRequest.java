package com.goldatech.paymentservice.web.dto.request.momo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RequestToPayRequest(
        String amount,
        String currency,
        String externalId,
        Payer payer,
        String payerMessage,
        String payeeNote
) {
}
