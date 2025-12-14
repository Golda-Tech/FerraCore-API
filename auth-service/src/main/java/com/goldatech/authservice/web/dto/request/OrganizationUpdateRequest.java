package com.goldatech.authservice.web.dto.request;

import com.goldatech.authservice.web.exception.PatternOrEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import org.springframework.lang.Nullable;

@Builder
public record OrganizationUpdateRequest(
        @Nullable String businessType,
        @Nullable String address,
        @Nullable String registrationNumber,
        @Nullable String taxId,
        @PatternOrEmpty(regexp = "^(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})([/\\w .-]*)*/?$",
                message = "Invalid website URL")
        @Nullable String website
) {
}
