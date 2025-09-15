package com.goldatech.paymentservice.domain.service;

import com.goldatech.paymentservice.domain.model.PaymentTransaction;
import com.goldatech.paymentservice.domain.model.events.PaymentEvent;
import com.goldatech.paymentservice.domain.repository.PaymentTransactionRepository;
import com.goldatech.paymentservice.domain.strategy.PaymentProvider;
import com.goldatech.paymentservice.domain.strategy.PaymentProviderFactory;
import com.goldatech.paymentservice.web.dto.request.NameEnquiryRequest;
import com.goldatech.paymentservice.web.dto.request.PaymentRequest;
import com.goldatech.paymentservice.web.dto.response.NameEnquiryResponse;
import com.goldatech.paymentservice.web.dto.response.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentProviderFactory providerFactory;
    private final PaymentTransactionRepository transactionRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${notification.exchange}")
    private String notificationExchange;

    @Value("${notification.payment.routing-key}")
    private String paymentRoutingKey;

    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {
        log.info("Initiating payment request for provider: {}", request.provider());
        PaymentProvider provider = providerFactory.getProvider(request.provider());
        PaymentTransaction transaction = provider.initiatePayment(request);


        //Publish event to RabbitMQ for asynchronous processing
        PaymentEvent event = new PaymentEvent(
                transaction.getTransactionRef(),
                transaction.getExternalRef(),
                transaction.getProvider(),
                transaction.getMobileNumber(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getStatus().toString(),
                transaction.getMessage(),
                "USER ID", // This should be replaced with actual user ID from context/session
                LocalDateTime.now()
        );

        log.info("Publishing payment event to RabbitMQ for transaction: {}", transaction.getTransactionRef());
        rabbitTemplate.convertAndSend(notificationExchange, paymentRoutingKey, event);


        return PaymentResponse.builder()
                .transactionRef(transaction.getTransactionRef())
                .externalRef(transaction.getExternalRef())
                .status(transaction.getStatus())
                .message(transaction.getMessage())
                .build();
    }

    public PaymentResponse checkPaymentStatus(String providerName, String transactionRef) {
        log.info("Checking payment status for transaction: {}", transactionRef);
        PaymentProvider provider = providerFactory.getProvider(providerName);
        PaymentTransaction updatedTransaction = provider.checkStatus(transactionRef);
        return PaymentResponse.builder()
                .transactionRef(updatedTransaction.getTransactionRef())
                .externalRef(updatedTransaction.getExternalRef())
                .status(updatedTransaction.getStatus())
                .message(updatedTransaction.getMessage())
                .build();
    }

    public Optional<NameEnquiryResponse> nameEnquiry(NameEnquiryRequest request) {
        log.info("Performing name enquiry for mobile number: {}", request.mobileNumber());
        PaymentProvider provider = providerFactory.getProvider("MTN"); // Name enquiry is often specific to the provider
        return provider.nameEnquiry(request);
    }
}
