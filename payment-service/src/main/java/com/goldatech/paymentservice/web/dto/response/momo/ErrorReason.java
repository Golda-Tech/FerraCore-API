package com.goldatech.paymentservice.web.dto.response.momo;

import com.goldatech.paymentservice.domain.model.ErrorCode;

public record ErrorReason(
        ErrorCode code,
        String message
) {
}
