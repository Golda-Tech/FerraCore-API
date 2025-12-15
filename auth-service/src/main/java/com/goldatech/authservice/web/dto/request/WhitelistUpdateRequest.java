package com.goldatech.authservice.web.dto.request;

import lombok.Builder;

@Builder
public record WhitelistUpdateRequest(
        String phone1,
        String phone2,
        String phone3,
        String phone4
) {
}
