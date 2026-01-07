package com.goldatech.paymentservice.web.dto.response.momo;


import com.goldatech.paymentservice.domain.model.MandateStatus;

public record PreApprovalResponse(
        String preApprovalRef,
        String message,
        MandateStatus status
) {
}
