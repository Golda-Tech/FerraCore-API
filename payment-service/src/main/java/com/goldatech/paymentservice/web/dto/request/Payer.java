package com.goldatech.paymentservice.web.dto.request;

public record Payer(
        String partyIdType,
        String partyId
) {
}
