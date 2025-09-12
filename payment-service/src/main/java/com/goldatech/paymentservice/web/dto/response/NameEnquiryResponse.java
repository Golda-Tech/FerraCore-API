package com.goldatech.paymentservice.web.dto.response;

import lombok.Builder;

@Builder
public record NameEnquiryResponse(
        String mobileNumber,
        String accountName,
        String accountNumber,
        String bankCode,
        String bankName,
        String message
) {
}
