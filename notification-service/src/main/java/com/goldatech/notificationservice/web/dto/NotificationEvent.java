package com.goldatech.notificationservice.web.dto;

import lombok.Builder;

import java.io.Serializable;
import java.util.Map;

@Builder
public record NotificationEvent(
        String eventType,
        String channel,
        String recipient,
        String userId,
        String subject,
        String body,
        Map<String, String> data
) implements Serializable {}
