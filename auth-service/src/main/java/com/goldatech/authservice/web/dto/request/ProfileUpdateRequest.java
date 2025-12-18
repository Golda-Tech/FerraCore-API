package com.goldatech.authservice.web.dto.request;

import lombok.Builder;

@Builder
public record ProfileUpdateRequest(
        String firstName,
        String lastName,
        String email,
        String phone
) {
}
