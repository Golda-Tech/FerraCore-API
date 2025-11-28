package com.goldatech.paymentservice.domain.service;

import com.goldatech.paymentservice.domain.exception.PreApprovalException;
import com.goldatech.paymentservice.domain.model.*;
import com.goldatech.paymentservice.domain.model.events.PaymentEvent;
import com.goldatech.paymentservice.domain.model.events.PreApprovalEvent;
import com.goldatech.paymentservice.domain.repository.MtnCallbackRepository;
import com.goldatech.paymentservice.domain.repository.OtpRepository;
import com.goldatech.paymentservice.domain.repository.PaymentTransactionRepository;
import com.goldatech.paymentservice.domain.repository.PreApprovalTransactionRepository;
import com.goldatech.paymentservice.domain.strategy.MtnPaymentProvider;
import com.goldatech.paymentservice.domain.strategy.PaymentProvider;
import com.goldatech.paymentservice.domain.strategy.PaymentProviderFactory;
import com.goldatech.paymentservice.web.dto.request.PaymentRequest;
import com.goldatech.paymentservice.web.dto.request.PreApprovalMandateRequest;
import com.goldatech.paymentservice.web.dto.request.momo.Payer;
import com.goldatech.paymentservice.web.dto.request.momo.PreApprovalRequest;
import com.goldatech.paymentservice.web.dto.response.PaymentResponse;
import com.goldatech.paymentservice.web.dto.response.PreApprovalMandateResponse;
import com.goldatech.paymentservice.web.dto.response.momo.PreApprovalResponse;
import com.goldatech.paymentservice.web.dto.response.momo.PreApprovalStatusResponse;
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
public class PreApprovalService {

    private final PaymentProviderFactory providerFactory;
    private final PreApprovalTransactionRepository preApprovalTransactionRepository;
    private final OtpRepository otpRepository;
    private final RabbitTemplate rabbitTemplate;
    private final MtnCallbackRepository callbackRepository;

    @Value("${notification.exchange}")
    private String notificationExchange;

    @Value("${notification.preapproval.routing-key}")
    private String preapprovalRoutingKey;

    @Value("${notification.otp.routing-key}")
    private String otpRoutingKey;

    @Transactional
    public PreApprovalMandateResponse creatPreApprovalMandate(PreApprovalMandateRequest request, String userId, String email) {
        log.info("Initiating payment request for provider: {}", request.provider());

        //Create PreApprovalTransaction in DB with PENDING status
        PreApprovalTransaction transaction = PreApprovalTransaction.builder()
                .externalRef(request.retrievalReference())
                .mobileNumber(request.mobileNumber())
                .provider(request.provider())
                .message(request.message())
                .validityTime(request.duration().toString())
                .status(MandateStatus.valueOf("PENDING"))
                .createdAt(LocalDateTime.now())
                .build();
        preApprovalTransactionRepository.save(transaction);

        PaymentProvider provider = providerFactory.getProvider(request.provider());

        //If instance of MTN, cast to MtnPaymentProvider to access specific methods
        if (!(provider instanceof com.goldatech.paymentservice.domain.strategy.MtnPaymentProvider)) {
            throw new IllegalArgumentException("Pre-approval mandates are only supported for MTN provider at the moment.");
        }

        //Create PreApprovalRequest from PreApprovalMandateRequest
        PreApprovalRequest preApprovalRequest = new PreApprovalRequest(
                new Payer(
                        "MSISDN",
                        request.mobileNumber()
                ),
                "GHS",
                request.message(),
                request.duration().toString()

        );

        Optional<PreApprovalResponse> response = ((MtnPaymentProvider) provider)
                .preApproval(preApprovalRequest);


        if (response.isEmpty()) {
            log.error("Failed to create pre-approval mandate with MTN for mobile number: {}", request.mobileNumber());
            throw new PreApprovalException("Failed to create pre-approval mandate with MTN.");
        }

        PreApprovalResponse preApprovalResponse = response.get();

        //Update transaction with response details
        transaction.setTransactionRef(preApprovalResponse.preApprovalRef());
        //transaction.setStatus(MandateStatus.valueOf(preApprovalResponse.status().toUpperCase()));
        transaction.setMessage(preApprovalResponse.message());
        transaction.setUpdatedAt(LocalDateTime.now());
        preApprovalTransactionRepository.save(transaction);

        //Publish event to RabbitMQ
        PreApprovalEvent event = new PreApprovalEvent(
                request.provider(),
                request.retrievalReference(),
                request.mobileNumber(),
                request.frequency(),
                request.duration(),
                request.reminders(),
                request.message()
        );


        log.info("Publishing payment event to RabbitMQ for transaction: {}", transaction.getTransactionRef());
        rabbitTemplate.convertAndSend(notificationExchange, preapprovalRoutingKey, event);

        return new PreApprovalMandateResponse(
                preApprovalResponse.preApprovalRef(),
                request.retrievalReference(),
                MandateStatus.PENDING,
                null,
                null
        );
    }


    //Check pre-approval status
    public PreApprovalStatusResponse checkPreApprovalStatus(String providerName, String mandateId) {
        log.info("Checking pre-approval status for provider: {} and mandateId: {}", providerName, mandateId);

        PaymentProvider provider = providerFactory.getProvider(providerName);

        //If instance of MTN, cast to MtnPaymentProvider to access specific methods
        if (!(provider instanceof com.goldatech.paymentservice.domain.strategy.MtnPaymentProvider)) {
            throw new IllegalArgumentException("Pre-approval mandates are only supported for MTN provider at the moment.");
        }

        Optional<PreApprovalStatusResponse> response = ((MtnPaymentProvider) provider)
                .checkPreApprovalStatus(mandateId);

        if (response.isEmpty()) {
            log.error("Failed to check pre-approval status with MTN for mandate id: {}", mandateId);
            throw new PreApprovalException("Failed to check pre-approval status with MTN.");
        }

        return response.get();
    }


    //Cancel pre-approval mandate
    public boolean cancelPreApprovalMandate(String providerName, String mandateId) {
        log.info("Cancelling pre-approval mandate for provider: {} and mandateId: {}", providerName, mandateId);

        PaymentProvider provider = providerFactory.getProvider(providerName);

        //If instance of MTN, cast to MtnPaymentProvider to access specific methods
        if (!(provider instanceof com.goldatech.paymentservice.domain.strategy.MtnPaymentProvider)) {
            throw new IllegalArgumentException("Pre-approval mandates are only supported for MTN provider at the moment.");
        }

        return ((MtnPaymentProvider) provider)
                .cancelPreApproval(mandateId);

    }
}
