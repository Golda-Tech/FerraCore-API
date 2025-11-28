package com.goldatech.paymentservice.domain.model.events;

import com.goldatech.paymentservice.domain.model.Frequency;
import jakarta.validation.constraints.NotBlank;

public record PreApprovalEvent(
        String provider,
        String retrievalReference,
        String mobileNumber,
        Frequency frequency,
        Long duration,
        Boolean reminders,
        String message
) {
}
