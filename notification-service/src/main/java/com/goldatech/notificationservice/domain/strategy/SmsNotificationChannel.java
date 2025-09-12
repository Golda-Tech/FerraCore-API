package com.goldatech.notificationservice.domain.strategy;

import com.goldatech.notificationservice.domain.model.NotificationLog;
import com.goldatech.notificationservice.domain.repository.NotificationLogRepository;
import com.goldatech.notificationservice.web.dto.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component("SMS")
@RequiredArgsConstructor
@Slf4j
public class SmsNotificationChannel implements NotificationChannel {
    private final NotificationLogRepository notificationLogRepository;

    @Override
    public void sendNotification(NotificationEvent event) {
        log.info("Attempting to send an SMS notification to: {}", event.recipient());

        // This is a placeholder for your actual SMS sending logic (e.g., calling a third-party API)
        boolean smsSentSuccessfully = true;
        String status = smsSentSuccessfully ? "SUCCESS" : "FAILED";
        String externalRef = UUID.randomUUID().toString();

        NotificationLog logEntry = NotificationLog.builder()
                .eventType(event.eventType())
                .recipient(event.recipient())
                .userId(event.userId())
                .channel("SMS")
                .message("SMS sent successfully. Message: " + event.body())
                .status(status)
                .timestamp(LocalDateTime.now())
                .externalRef(externalRef)
                .build();

        notificationLogRepository.save(logEntry);
        log.info("SMS notification sent and logged with ID: {}", logEntry.getId());
    }
}
