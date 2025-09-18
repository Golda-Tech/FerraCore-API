package com.goldatech.notificationservice.domain.service;

import com.goldatech.notificationservice.domain.strategy.NotificationChannel;
import com.goldatech.notificationservice.domain.strategy.NotificationChannelFactory;
import com.goldatech.notificationservice.web.dto.*;
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

        if ("SUCCESSFUL".equalsIgnoreCase(event.status())) {
            NotificationChannel smsChannel = channelFactory.getChannel("SMS");
            String messageBody = String.format("Hi, a payment of %s %s has been successfully processed. Ref: %s",
                    event.amount(), event.currency(), event.transactionRef());
            smsChannel.sendNotification(new NotificationEvent(
                    "PAYMENT_SUCCESS", "SMS", event.mobileNumber(), event.userId(), "Payment Success", messageBody, null
            ));

            //Send email to user
//            NotificationChannel emailChannel = channelFactory.getChannel("EMAIL");
//            String emailBody = String.format("Dear User,\n\nYour payment of %s %s has been successfully processed.\nTransaction Reference: %s\n\nThank you for using our services.\n\nBest regards,\nYour Company",
//                    event.amount(), event.currency(), event.transactionRef());
//            emailChannel.sendNotification(new NotificationEvent(
//                    "PAYMENT_SUCCESS_EMAIL", "EMAIL", event.email(), event.userId(), "Payment Success", emailBody, null
//            ));

        } else if ("PENDING".equalsIgnoreCase(event.status())) {
            NotificationChannel smsChannel = channelFactory.getChannel("SMS");
            String messageBody = String.format("Your payment of %s %s is currently pending. Ref: %s",
                    event.amount(), event.currency(), event.transactionRef());
            smsChannel.sendNotification(new NotificationEvent(
                    "PAYMENT_PENDING", "SMS", event.mobileNumber(), event.userId(), "Payment Pending", messageBody, null
            ));

//            NotificationChannel emailChannel = channelFactory.getChannel("EMAIL");
//            String emailBody = String.format("Dear User,\n\nYour payment of %s %s is currently pending.\nTransaction Reference: %s\n\nWe will notify you once the status changes.\n\nBest regards,\nYour Company",
//                    event.amount(), event.currency(), event.transactionRef());
//            emailChannel.sendNotification(new NotificationEvent(
//                    "PAYMENT_PENDING_EMAIL", "EMAIL", event.email(), event.userId(), "Payment Pending", emailBody, null
//            ));
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


    @RabbitListener(queues = "${notification.otp.queue}")
    @Transactional
    public void handleOtpEvent(OtpEvent event) {
        log.info("Received OtpEvent for userId: {}", event.userId());


        if ("PAYMENT".equalsIgnoreCase(event.type())) {

            if ("SMS".equalsIgnoreCase(event.channel())) {
                NotificationChannel smsChannel = channelFactory.getChannel("SMS");
                String messageBody = String.format("Your payment OTP is: %s. It is valid for 10 minutes.",
                        event.otpCode());
                smsChannel.sendNotification(new NotificationEvent(
                        "PAYMENT_OTP", "SMS", event.mobileNumber(), event.userId(),
                        "Payment OTP", messageBody, null
                ));
            } else if ("EMAIL".equalsIgnoreCase(event.channel())) {
                NotificationChannel emailChannel = channelFactory.getChannel("EMAIL");
                String emailBody = String.format("Your payment OTP is: %s. It is valid for 10 minutes.",
                        event.otpCode());
                emailChannel.sendNotification(new NotificationEvent(
                        "PAYMENT_OTP_EMAIL", "EMAIL", event.email(), event.userId(),
                        "Payment OTP", emailBody, null
                ));
            }


        } else if ("LOGIN".equalsIgnoreCase(event.type())) {
            if (event.channel().equals("EMAIL")) {
                NotificationChannel smsChannel = channelFactory.getChannel("SMS");
                String messageBody = String.format("Your login OTP is: %s. It is valid for 10 minutes.",
                        event.otpCode());
                smsChannel.sendNotification(new NotificationEvent(
                        "LOGIN_OTP", "SMS", event.mobileNumber(), event.userId(), "Login OTP", messageBody, null
                ));
            } else {
                NotificationChannel emailChannel = channelFactory.getChannel("EMAIL");
                String emailBody = String.format("Your login OTP is: %s. It is valid for 10 minutes.",
                        event.otpCode());
                emailChannel.sendNotification(new NotificationEvent(
                        "LOGIN_OTP_EMAIL", "EMAIL", event.email(), event.userId(), "Login OTP", emailBody, null
                ));
            }


        }
    }
}