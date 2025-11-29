package com.goldatech.paymentservice.web.dto.request.momo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Payer(
        String partyIdType,
        String partyId
) {}
