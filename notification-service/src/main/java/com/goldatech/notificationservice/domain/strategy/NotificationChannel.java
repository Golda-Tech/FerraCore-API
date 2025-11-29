package com.goldatech.notificationservice.domain.strategy;

import com.goldatech.notificationservice.web.dto.NotificationEvent;

public interface NotificationChannel {
    void sendNotification(NotificationEvent event);
}