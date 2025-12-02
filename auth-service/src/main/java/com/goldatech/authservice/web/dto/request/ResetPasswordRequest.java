package com.goldatech.authservice.web.dto.request;

public record ResetPasswordRequest(
        String email,
        String tempPassword,
        String newPassword
) {
}
