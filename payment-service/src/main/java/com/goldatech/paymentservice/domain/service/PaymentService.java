package com.goldatech.paymentservice.domain.service;

import com.goldatech.paymentservice.domain.model.Otp;
import com.goldatech.paymentservice.domain.model.PaymentTransaction;
import com.goldatech.paymentservice.domain.model.events.OtpEvent;
import com.goldatech.paymentservice.domain.model.events.PaymentEvent;
import com.goldatech.paymentservice.domain.repository.OtpRepository;
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
    private final OtpRepository otpRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${notification.exchange}")
    private String notificationExchange;

    @Value("${notification.payment.routing-key}")
    private String paymentRoutingKey;

    @Value("${notification.otp.routing-key}")
    private String otpRoutingKey;

    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request, String userId, String email) {
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
                userId,
                email,
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


    //Get all payments
    public Iterable<PaymentTransaction> getAllPayments() {
        log.info("Getting all payment transactions");
        return transactionRepository.findAll();
    }

    //Get payment by transaction reference
    public Optional<PaymentTransaction> getPaymentByTransactionRef(String transactionRef) {
        log.info("Getting payment transaction by reference: {}", transactionRef);
        return transactionRepository.findByTransactionRef(transactionRef);

    }


    //OTP before payment
    public boolean sendOtp(String mobileNumber) {
        log.info("Sending OTP to mobile number: {}", mobileNumber);
        //Publish OTP event to RabbitMQ for asynchronous processing

        //Generate OTP
        String generatedOtp = otpGenerator();
        log.info("Generated OTP: {} for mobile number: {}", generatedOtp, mobileNumber);

        //Save OTP to database with expiry
        Otp otp = Otp.builder()
                .mobileNumber(mobileNumber)
                .otpCode(generatedOtp)
                .type("PAYMENT_OTP")
                .message("Your OTP code is " + generatedOtp)
                .channel("SMS")
                .subject("Payment OTP Verification")
                .used(false)
                .build();

        otpRepository.save(otp);

        OtpEvent event = new OtpEvent(
                mobileNumber,
                null,
                generatedOtp,
                null,
                "PAYMENT",
                "Your payment OTP is: " + generatedOtp + ". It is valid for 5 minutes.",
                "SMS",
                "Payment OTP"
        );

        rabbitTemplate.convertAndSend(notificationExchange, otpRoutingKey, event);
        log.info("Published OTP event to RabbitMQ for mobile number: {}", mobileNumber);

        return true;
    }

    //Verify OTP
    public boolean verifyOtp(String mobileNumber, String otp) {
        log.info("Verifying OTP for mobile number: {}", mobileNumber);
        Optional<Otp> otpRecordOpt = otpRepository.findTopByMobileNumberAndOtpCodeOrderByCreatedAtDesc(mobileNumber, otp);
        if (otpRecordOpt.isEmpty()) {
            log.warn("No OTP record found for mobile number: {}", mobileNumber);
            return false;
        }
        Otp otpRecord = otpRecordOpt.get();
        if (otpRecord.isUsed()) {
            log.warn("OTP already used for mobile number: {}", mobileNumber);
            return false;
        }

        if (otpRecord.isExpired()) {
            log.warn("OTP expired for mobile number: {}", mobileNumber);
            return false;
        }

        //Mark OTP as used
        otpRecord.setUsed(true);
        otpRepository.save(otpRecord);
        log.info("OTP verified successfully for mobile number: {}", mobileNumber);
        return true;
    }


    public String otpGenerator(){
        //Generate a random 6-digit OTP
        int otp = (int)(Math.random() * 900000) + 100000;
        return String.valueOf(otp);
    }
}
