package com.goldatech.notificationservice.web.dto.response;

import java.util.Map;

public record SmsResponse(
        boolean success,
        int code,
        String message,
        SmsData data
) {}
