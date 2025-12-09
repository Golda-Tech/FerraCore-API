package com.goldatech.authservice.web.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record OrganizationUpdateRequest(
        String businessType,
        String address,
        String registrationNumber,
        String taxId,
        @Pattern(regexp = "^(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})([/\\w .-]*)*/?$",
                message = "Invalid website URL")
        String website
) {
}
