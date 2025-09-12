package com.goldatech.notificationservice.domain.strategy;

import com.goldatech.notificationservice.domain.model.NotificationLog;
import com.goldatech.notificationservice.domain.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component("EMAIL")
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationChannel implements NotificationChannel {
    private final NotificationLogRepository notificationLogRepository;

    @Override
    public void sendNotification(NotificationEvent event) {
        log.info("Attempting to send an email notification to: {}", event.recipient());

        boolean emailSentSuccessfully = true;
        String status = emailSentSuccessfully ? "SUCCESS" : "FAILED";
        String externalRef = UUID.randomUUID().toString();

        NotificationLog logEntry = NotificationLog.builder()
                .eventType(event.eventType())
                .recipient(event.recipient())
                .userId(event.userId())
                .channel("EMAIL")
                .message("Email sent successfully. Subject: " + event.subject())
                .status(status)
                .timestamp(LocalDateTime.now())
                .externalRef(externalRef)
                .build();

        notificationLogRepository.save(logEntry);
        log.info("Email notification sent and logged with ID: {}", logEntry.getId());
    }
}