package com.goldatech.notificationservice.web.dto.response;

public record SmsData(
        String key,
        String to,
        String msg,
        String sender_id,
        String campaign_id
) {
}
