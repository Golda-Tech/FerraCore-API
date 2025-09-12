package com.goldatech.paymentservice.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record NameEnquiryRequest(
        @NotBlank String mobileNumber
) {}

