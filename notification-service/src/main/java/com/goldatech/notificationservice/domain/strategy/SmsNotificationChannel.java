package com.goldatech.notificationservice.domain.strategy;

import com.goldatech.notificationservice.domain.model.NotificationLog;
import com.goldatech.notificationservice.domain.repository.NotificationLogRepository;
import com.goldatech.notificationservice.web.dto.NotificationEvent;
import com.goldatech.notificationservice.web.dto.response.SmsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Component("SMS")
@RequiredArgsConstructor
@Slf4j
public class SmsNotificationChannel implements NotificationChannel {
    private final NotificationLogRepository notificationLogRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${sms.api.key}")
    private String SMS_API_KEY;

    @Value("${sms.sender.id}")
    private String SMS_SENDER_ID;

    @Value("${sms.base-url}")
    private String SMS_BASE_URL;

    @Override
    public void sendNotification(NotificationEvent event) {
        log.info("Attempting to send an SMS notification to: {}", event.recipient());

        String phoneNumber = event.recipient();
        String messageBody = event.body();

        boolean smsSentSuccessfully = false;
        SmsResponse smsResponse = null;

        try {
            // Build full request URL with query params
            String url = UriComponentsBuilder.fromHttpUrl(SMS_BASE_URL)
                    .queryParam("key", SMS_API_KEY)
                    .queryParam("to", phoneNumber)
                    .queryParam("msg", messageBody)
                    .queryParam("sender_id", SMS_SENDER_ID)
                    .toUriString();

            log.debug("SMS API request URL: {}", url);

            // Call SMS API and map JSON into SmsResponse
            ResponseEntity<SmsResponse> response =
                    restTemplate.getForEntity(url, SmsResponse.class);

            smsResponse = response.getBody();
            smsSentSuccessfully = response.getStatusCode().is2xxSuccessful()
                    && smsResponse != null
                    && smsResponse.success();

            log.info("SMS API Response: {}", smsResponse);

        } catch (Exception e) {
            log.error("Failed to send SMS to {}. Error: {}", phoneNumber, e.getMessage(), e);
        }

        String status = smsSentSuccessfully ? "SUCCESS" : "FAILED";

        NotificationLog logEntry = NotificationLog.builder()
                .eventType(event.eventType())
                .recipient(event.recipient())
                .userId(event.userId())
                .channel("SMS")
                .message(smsResponse != null
                        ? "SMS API response: " + smsResponse.message()
                        : "No response from SMS API")
                .status(status)
                .timestamp(LocalDateTime.now())
                .externalRef(
                        smsResponse != null && smsResponse.data() != null
                                ? smsResponse.data().campaign_id()
                                : UUID.randomUUID().toString()
                )
                .build();

        notificationLogRepository.save(logEntry);
        log.info("SMS notification logged with ID: {}", logEntry.getId());
    }

}
