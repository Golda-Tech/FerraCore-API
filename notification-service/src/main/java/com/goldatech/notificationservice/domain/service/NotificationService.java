package com.goldatech.notificationservice.domain.service;

import com.goldatech.notificationservice.domain.strategy.NotificationChannel;
import com.goldatech.notificationservice.domain.strategy.NotificationChannelFactory;
import com.goldatech.notificationservice.web.dto.AuthEvent;
import com.goldatech.notificationservice.web.dto.CollectionEvent;
import com.goldatech.notificationservice.web.dto.NotificationEvent;
import com.goldatech.notificationservice.web.dto.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationChannelFactory channelFactory;

    @RabbitListener(queues = "${notification.payment.queue}")
    @Transactional
    public void handlePaymentEvent(PaymentEvent event) {
        log.info("Received PaymentEvent for transaction ref: {}", event.transactionRef());

        if ("SUCCESS".equalsIgnoreCase(event.status())) {
            NotificationChannel smsChannel = channelFactory.getChannel("SMS");
            String messageBody = String.format("Hi, a payment of %s %s has been successfully processed. Ref: %s",
                    event.amount(), event.currency(), event.transactionRef());
            smsChannel.sendNotification(new NotificationEvent(
                    "PAYMENT_SUCCESS", "SMS", event.mobileNumber(), event.userId(), "Payment Success", messageBody, null
            ));
        } else if ("FAILED".equalsIgnoreCase(event.status())) {
            NotificationChannel emailChannel = channelFactory.getChannel("EMAIL");
            String messageBody = String.format("Payment failed for transaction ref %s. Reason: %s",
                    event.transactionRef(), event.message());
            emailChannel.sendNotification(new NotificationEvent(
                    "PAYMENT_FAILED", "EMAIL", "support@yourcompany.com", event.userId(), "Payment Failed Alert", messageBody, null
            ));
        }
    }

    @RabbitListener(queues = "${notification.collection.queue}")
    @Transactional
    public void handleCollectionEvent(CollectionEvent event) {
        log.info("Received CollectionEvent for collection ref: {}", event.collectionRef());

        if ("COLLECTION_SUCCESS".equalsIgnoreCase(event.status())) {
            NotificationChannel smsChannel = channelFactory.getChannel("SMS");
            String messageBody = String.format("A collection of %s has been completed. Ref: %s",
                    event.amount(), event.collectionRef());
            smsChannel.sendNotification(new NotificationEvent(
                    "COLLECTION_SUCCESS", "SMS", event.mobileNumber(), event.userId(), "Collection Success", messageBody, null
            ));
        } else if ("COLLECTION_FAILED".equalsIgnoreCase(event.status())) {
            NotificationChannel emailChannel = channelFactory.getChannel("EMAIL");
            String messageBody = String.format("Collection failed for ref %s. Reason: %s. Please review.",
                    event.collectionRef(), event.message());
            emailChannel.sendNotification(new NotificationEvent(
                    "COLLECTION_FAILED", "EMAIL", "finance@yourcompany.com", event.userId(), "Collection Failed Alert", messageBody, null
            ));
        }
    }

    @RabbitListener(queues = "${notification.auth.queue}")
    @Transactional
    public void handleAuthEvent(AuthEvent event) {
        log.info("Received AuthEvent for username: {}", event.username());

        if ("USER_CREATED".equalsIgnoreCase(event.eventAction())) {
            NotificationChannel emailChannel = channelFactory.getChannel("EMAIL");
            String messageBody = String.format("Welcome to the platform, %s! Your account has been created.",
                    event.username());
            emailChannel.sendNotification(new NotificationEvent(
                    "USER_CREATED", "EMAIL", event.email(), event.userId(), "Welcome!", messageBody, null
            ));
        } else if ("PASSWORD_RESET".equalsIgnoreCase(event.eventAction())) {
            NotificationChannel emailChannel = channelFactory.getChannel("EMAIL");
            String messageBody = String.format("A password reset has been requested for your account.");
            emailChannel.sendNotification(new NotificationEvent(
                    "PASSWORD_RESET", "EMAIL", event.email(), event.userId(), "Password Reset", messageBody, null
            ));
        }
    }
}