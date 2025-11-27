package com.goldatech.paymentservice.web.dto.request;

import com.goldatech.paymentservice.web.dto.request.momo.Payer;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PreApprovalMandateRequest (
    @NotBlank String provider,
    @NotBlank String transactionReference,
    @NotBlank String mobileNumber,
    @NotBlank String payerCurrency,
    @NotBlank String payerMessage,
    @NotBlank String validityTime
) {}