package com.goldatech.paymentservice.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PaymentRequest(
        @NotBlank String provider,
        @NotBlank String collectionRef,
        @NotBlank String merchantId,
        @NotBlank String mobileNumber,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String currency
) {}
