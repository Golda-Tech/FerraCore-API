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
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@Component("SMS")
@RequiredArgsConstructor
@Slf4j
public class SmsNotificationChannel implements NotificationChannel {
    private final NotificationLogRepository notificationLogRepository;
    private final WebClient webClient = WebClient.create();

    @Value("${sms.api.key}")
    private String SMS_API_KEY;

    @Value("${sms.sender.id}")
    private String SMS_SENDER_ID;

    @Value("${sms.base-url}")
    private String SMS_BASE_URL;

    @Override
    public void sendNotification(NotificationEvent event) {
        String phoneNumber = event.recipient();
        String messageBody = event.body();

        try {
            Mono<SmsResponse> responseMono = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host(SMS_BASE_URL.replace("https://", ""))
                            .path("/smsapi")
                            .queryParam("key", SMS_API_KEY)
                            .queryParam("to", phoneNumber)
                            .queryParam("msg", messageBody)
                            .queryParam("sender_id", SMS_SENDER_ID)
                            .build()
                    )
                    .retrieve()
                    .bodyToMono(SmsResponse.class);

            SmsResponse smsResponse = responseMono.block(); // blocking for simplicity
            log.info("SMS API Response: {}", smsResponse);

            // Log notification
            NotificationLog logEntry = NotificationLog.builder()
                    .eventType(event.eventType())
                    .recipient(event.recipient())
                    .userId(event.userId())
                    .channel("SMS")
                    .message(smsResponse != null ? smsResponse.message() : "No response from SMS API")
                    .status(smsResponse != null && smsResponse.success() ? "SUCCESS" : "FAILED")
                    .timestamp(LocalDateTime.now())
                    .externalRef(smsResponse != null && smsResponse.data() != null
                            ? smsResponse.data().campaign_id()
                            : UUID.randomUUID().toString())
                    .build();

            notificationLogRepository.save(logEntry);
            log.info("SMS notification logged with ID: {}", logEntry.getId());

        } catch (Exception e) {
            log.error("Failed to send SMS to {}. Error: {}", phoneNumber, e.getMessage(), e);
        }
    }

}
