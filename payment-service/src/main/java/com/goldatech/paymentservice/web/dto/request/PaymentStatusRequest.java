package com.goldatech.paymentservice.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PaymentStatusRequest(
        @NotBlank String provider,
        @NotBlank String transactionRef
) {}
