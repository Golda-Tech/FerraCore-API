package com.goldatech.notificationservice.domain.strategy;

import com.goldatech.notificationservice.domain.model.NotificationLog;
import com.goldatech.notificationservice.domain.repository.NotificationLogRepository;
import com.goldatech.notificationservice.web.dto.NotificationEvent;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component("EMAIL")
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationChannel implements NotificationChannel {
    private final NotificationLogRepository notificationLogRepository;

    @Value("${resend.api.key}")
    private String RESEND_API_KEY;

    @Override
    public void sendNotification(NotificationEvent event) {
        log.info("Attempting to send email notification to: {}", event.recipient());

        Resend resend = new Resend(RESEND_API_KEY);

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("RexHub <no-reply@rexhub.com.gh>")
                .to(event.recipient())
                .subject(event.subject())
                .html(event.body())
                .build();

        boolean emailSentSuccessfully = false;
        String externalRef = UUID.randomUUID().toString();

        try {
            CreateEmailResponse response = resend.emails().send(params);
            log.info("Email sent successfully. Response ID: {}", response.getId());
            emailSentSuccessfully = true;
        } catch (ResendException e) {
            log.error("Failed to send email notification to {}", event.recipient(), e);
        }

        String status = emailSentSuccessfully ? "SUCCESS" : "FAILED";

        NotificationLog logEntry = NotificationLog.builder()
                .eventType(event.eventType())
                .recipient(event.recipient())
                .userId(event.userId())
                .channel("EMAIL")
                .message("Email " + status.toLowerCase() + ". Subject: " + event.subject())
                .status(status)
                .timestamp(LocalDateTime.now())
                .externalRef(externalRef)
                .build();

        notificationLogRepository.save(logEntry);
        log.info("Notification log saved with ID: {}", logEntry.getId());
    }

}