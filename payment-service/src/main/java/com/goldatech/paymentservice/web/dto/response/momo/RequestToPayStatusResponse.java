package com.goldatech.paymentservice.web.dto.response.momo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.goldatech.paymentservice.web.dto.request.momo.Payer;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RequestToPayStatusResponse(
        String status,
        String reason,
        String amount,
        String currency,
        @JsonProperty("financialTransactionId") String financialTransactionId,
        @JsonProperty("externalId") String externalId,
        Payer payer,
        String payerMessage,
        String payeeNote
) {}
