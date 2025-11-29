package com.goldatech.paymentservice.web.dto.request.momo;

import jakarta.validation.constraints.NotBlank;

public record PreApprovalRequest(
        @NotBlank Payer payer,
        @NotBlank String payerCurrency,
        @NotBlank String payerMessage,
        @NotBlank String validityTime
) {
}
