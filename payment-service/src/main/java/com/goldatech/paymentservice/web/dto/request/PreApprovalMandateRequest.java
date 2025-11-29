package com.goldatech.paymentservice.web.dto.request;

import com.goldatech.paymentservice.domain.model.Frequency;
import com.goldatech.paymentservice.web.dto.request.momo.Payer;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PreApprovalMandateRequest (
        @NotBlank String provider,
        @NotBlank String retrievalReference,
        @NotBlank String mobileNumber,
        @NotNull Frequency frequency,
        @NotNull @Positive Long duration,//Duration of mandate in seconds. e.g. "86400" for 1 day
        @NotNull Boolean reminders,
        @NotBlank String message
) {}