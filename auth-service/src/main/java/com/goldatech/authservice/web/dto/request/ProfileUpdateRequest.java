package com.goldatech.authservice.web.dto.request;

import com.goldatech.authservice.domain.model.Role;
import com.goldatech.authservice.web.dto.response.UserProfileResponse;
import lombok.Builder;

@Builder
public record ProfileUpdateRequest(
        String firstName,
        String lastName,
        String email,
        String phone
) {
}
