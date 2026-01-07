package com.goldatech.paymentservice.web.dto.response;

import com.goldatech.paymentservice.domain.model.MandateStatus;
import com.goldatech.paymentservice.domain.model.TransactionStatus;

import java.time.LocalDateTime;

public record PreApprovalResponse(
        String mandateId,//provided by the Ferracore upon successful response from the telco. This is after a successful get status from the telco
        String retrievalReference,//e.g. "abc-123", –provided by client calling Ferracore,
        MandateStatus status//e.g. "PENDING", "ACTIVE", "EXPIRED", "CANCELLED"
) {
}
