package com.goldatech.notificationservice.domain.strategy;

public interface NotificationChannel {
    void sendNotification(NotificationEvent event);
}