package com.goldatech.paymentservice.web.dto.response;

import com.goldatech.paymentservice.domain.model.MandateStatus;
import com.goldatech.paymentservice.domain.model.TransactionStatus;

import java.time.LocalDateTime;

public record PreApprovalCancelResponse(
        String code,
        String message,
        String mandateId
) {
}
