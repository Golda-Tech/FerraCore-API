package com.goldatech.paymentservice.web.dto.response;

import com.goldatech.paymentservice.domain.model.TransactionStatus;
import lombok.Builder;

@Builder
public record PaymentResponse(
        String transactionRef,
        String externalRef,
        TransactionStatus status,
        String message
) {}
