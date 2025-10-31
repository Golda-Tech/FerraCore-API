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
import com.goldatech.paymentservice.web.dto.response.PaymentTrendDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

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

    public enum Interval {
        DAILY, WEEKLY, MONTHLY
    }

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
        return transactionRepository.findAll(Sort.by(Sort.Direction.DESC, "initiatedAt"));
    }

    //Get payment by transaction reference
    public Optional<PaymentTransaction> getPaymentByTransactionRef(String transactionRef) {
        log.info("Getting payment transaction by reference: {}", transactionRef);
        return transactionRepository.findByTransactionRef(transactionRef);

    }


    public Map<String, Long> getPaymentStatusSummary() {
        log.info("Getting payment status summary");
        return transactionRepository.findAll().stream()
                .collect(Collectors.groupingBy(t -> t.getStatus().name(), Collectors.counting()));
    }

    public List<PaymentTrendDTO> getPaymentTrends(LocalDateTime start, LocalDateTime end, Interval interval) {
        List<PaymentTransaction> transactions = transactionRepository.findByInitiatedAtBetween(start, end);

        // Choose grouping function
        java.util.function.Function<PaymentTransaction, java.time.LocalDate> groupingFn = switch (interval) {
            case DAILY -> t -> t.getInitiatedAt().toLocalDate();
            case WEEKLY -> t -> t.getInitiatedAt().toLocalDate()
                    .with(java.time.DayOfWeek.MONDAY); // start of week
            case MONTHLY -> t -> t.getInitiatedAt().toLocalDate()
                    .withDayOfMonth(1); // start of month
        };

        // Group by interval
        Map<java.time.LocalDate, List<PaymentTransaction>> grouped = transactions.stream()
                .collect(Collectors.groupingBy(groupingFn));

        return grouped.entrySet().stream()
                .map(entry -> {
                    java.time.LocalDate period = entry.getKey();
                    List<PaymentTransaction> groupTransactions = entry.getValue();

                    long totalCount = groupTransactions.size();
                    double totalAmount = groupTransactions.stream()
                            .mapToDouble(t -> t.getAmount().doubleValue())
                            .sum();

                    Map<String, Long> statusCounts = groupTransactions.stream()
                            .collect(Collectors.groupingBy(
                                    t -> t.getStatus().name(),
                                    Collectors.counting()
                            ));

                    return new PaymentTrendDTO(period, totalCount, new BigDecimal(totalAmount), new HashMap<>(), statusCounts);
                })
                .sorted(java.util.Comparator.comparing(PaymentTrendDTO::date))
                .toList();
    }


    //OTP before payment
    public boolean sendOtp(String destination, String channel, String type) {
        String generatedOtp = otpGenerator();

        Otp otp = Otp.builder()
                .otpCode(generatedOtp)
                .type(type)
                .message("Your OTP code is " + generatedOtp)
                .channel(channel)
                .subject(type + " Verification")
                .used(false)
                .build();

        if ("SMS".equals(channel)) {
            otp.setMobileNumber(destination);
        } else if ("EMAIL".equals(channel)) {
            otp.setEmail(destination);
        }

        otpRepository.save(otp);

        OtpEvent event = new OtpEvent(
                "SMS".equals(channel) ? destination : null,
                "EMAIL".equals(channel) ? destination : null,
                generatedOtp,
                null,
                type,
                otp.getMessage(),
                channel,
                otp.getSubject()
        );

        rabbitTemplate.convertAndSend(notificationExchange, otpRoutingKey, event);

        return true;
    }



    //Verify OTP
    @Transactional
    public boolean verifyOtp(String identifier, String otp, String channel) {
        log.info("Verifying OTP for {}: {}", channel, identifier);

        Optional<Otp> otpRecordOpt;

        if ("SMS".equalsIgnoreCase(channel)) {
            otpRecordOpt = otpRepository
                    .findTopByMobileNumberAndOtpCodeAndExpiresAtAfterAndUsedFalseOrderByCreatedAtDesc(
                            identifier, otp, LocalDateTime.now()
                    );
        } else if ("EMAIL".equalsIgnoreCase(channel)) {
            otpRecordOpt = otpRepository
                    .findTopByEmailAndOtpCodeAndExpiresAtAfterAndUsedFalseOrderByCreatedAtDesc(
                            identifier, otp, LocalDateTime.now()
                    );
        } else {
            log.error("Unsupported OTP channel: {}", channel);
            throw new IllegalArgumentException("Unsupported OTP channel: " + channel);
        }

        if (otpRecordOpt.isEmpty()) {
            log.warn("No valid OTP found for {}: {}", channel, identifier);
            return false;
        }

        Otp otpRecord = otpRecordOpt.get();

        // Extra defensive checks (even though query excludes expired/used)
        if (otpRecord.isUsed()) {
            log.warn("OTP already used for {}: {}", channel, identifier);
            return false;
        }

        if (otpRecord.isExpired()) {
            log.warn("OTP expired for {}: {}", channel, identifier);
            return false;
        }

        // Mark OTP as used
        otpRecord.setUsed(true);
        otpRepository.save(otpRecord);

        log.info("OTP verified successfully for {}: {}", channel, identifier);
        return true;
    }



    private String otpGenerator() {
        int otp = ThreadLocalRandom.current().nextInt(100000, 999999); // 6 digits
        return String.valueOf(otp);
    }

}
