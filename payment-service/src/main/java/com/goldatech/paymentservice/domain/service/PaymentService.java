package com.goldatech.paymentservice.domain.service;

import com.goldatech.paymentservice.domain.model.Subscription;
import com.goldatech.paymentservice.domain.repository.*;
import com.goldatech.paymentservice.domain.model.MtnCallback;
import com.goldatech.paymentservice.domain.model.Otp;
import com.goldatech.paymentservice.domain.model.PaymentTransaction;
import com.goldatech.paymentservice.domain.model.TransactionStatus;
import com.goldatech.paymentservice.domain.model.events.OtpEvent;
import com.goldatech.paymentservice.domain.model.events.PaymentEvent;
import com.goldatech.paymentservice.domain.strategy.PaymentProvider;
import com.goldatech.paymentservice.domain.strategy.PaymentProviderFactory;
import com.goldatech.paymentservice.web.dto.request.MtnCallBackRequest;
import com.goldatech.paymentservice.web.dto.request.NameEnquiryRequest;
import com.goldatech.paymentservice.web.dto.request.PaymentRequest;
import com.goldatech.paymentservice.web.dto.response.NameEnquiryResponse;
import com.goldatech.paymentservice.web.dto.response.PaymentResponse;
import com.goldatech.paymentservice.web.dto.response.PaymentTrendDTO;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static jakarta.transaction.Transactional.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentProviderFactory providerFactory;
    private final PaymentTransactionRepository transactionRepository;
    private final PartnerSummaryRepository partnerSummaryRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final OtpRepository otpRepository;
    private final RabbitTemplate rabbitTemplate;
    private final MtnCallbackRepository callbackRepository;
    private final EntityManager em;

    @Value("${notification.exchange}")
    private String notificationExchange;

    @Value("${notification.payment.routing-key}")
    private String paymentRoutingKey;

    @Value("${notification.otp.routing-key}")
    private String otpRoutingKey;

    @Value("${spring.application.environment}")
    private String environment;


    public enum Interval {
        DAILY, WEEKLY, MONTHLY
    }


    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request, String callbackUrl, String xRef,  String targetEnvironment) {
        log.info("Initiating payment request for provider: {}", request.provider());
        PaymentProvider provider = providerFactory.getProvider(request.provider());

        //verify if environment from header matches application environment
        if (!this.environment.equalsIgnoreCase(targetEnvironment)) {
            log.error("Target environment {} does not match application environment {}", targetEnvironment, this.environment);
            throw new IllegalArgumentException("Target environment does not match application environment");
        }

        //verify if xRef is not null or empty
        if (xRef == null || xRef.isEmpty()) {
            log.error("X-Reference-Id header is missing or empty");
            throw new IllegalArgumentException("X-Reference-Id header is required");
        }

        //verify if xRef is an actual UUID
        try {
            UUID.fromString(xRef);
        } catch (IllegalArgumentException e) {
            log.error("X-Reference-Id {} is not a valid UUID", xRef);
            throw new IllegalArgumentException("X-Reference-Id must be a valid UUID");
        }

        //verify if callbackUrl is a valid URL and starts with http or https
        if (callbackUrl == null || (!callbackUrl.startsWith("http://") && !callbackUrl.startsWith("https://"))) {
            log.error("Callback URL {} is not valid", callbackUrl);
            throw new IllegalArgumentException("Callback URL must be a valid URL starting with http:// or https://");
        }

        //verify if mobile number is whitelisted in the subscription record
        String mobileNumber = request.mobileNumber();

        Optional<Subscription> subscriptionOpt = subscriptionRepository
                .findByContactEmailAndWhitelistedNumbers(request.initiatedBy(),mobileNumber);

        if (subscriptionOpt.isEmpty()) {
            log.error("Mobile number {} is not whitelisted in any subscription", mobileNumber);
            throw new IllegalArgumentException(
                    "Mobile number is not whitelisted. Please update your whitelisted mobile numbers via API or on the RexHub Dashboard.");
        }



        PaymentTransaction transaction = provider.initiatePayment(request, callbackUrl, xRef);


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

    public List<PaymentTransaction> getAllPayments(String initiatedBy) {
        log.info("Getting all payments for user: {}", initiatedBy);
        return transactionRepository.findByInitiatedByOrderByInitiatedAtDesc(initiatedBy);
    }

    //Get payment by transaction reference
    public Optional<PaymentTransaction> getPaymentByTransactionRef(String transactionRef) {
        log.info("Getting payment transaction by reference: {}", transactionRef);
        return transactionRepository.findByTransactionRef(transactionRef);

    }


    public Map<String, Long> getPaymentStatusSummary() {
        return transactionRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(
                        t -> t.getStatus().name(),
                        Collectors.counting()
                ));
    }

    public Map<String, Long> getPaymentStatusSummary(String initiatedBy) {
        log.info("Getting payment status summary for user: {}", initiatedBy);
        return transactionRepository.findByInitiatedBy(initiatedBy)
                .stream()
                .collect(Collectors.groupingBy(
                        t -> t.getStatus().name(),
                        Collectors.counting()
                ));
    }

    public List<PaymentTrendDTO> getPaymentTrends(String initBy, LocalDateTime start, LocalDateTime end, Interval interval) {
        List<PaymentTransaction> transactions = transactionRepository.findByInitiatedAtBetweenAndInitiatedBy(start, end, initBy);

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
                            .filter(t -> t.getStatus() != null && t.getStatus() == TransactionStatus.SUCCESSFUL)
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


    @Transactional
    private void updatePartnerSummary(String partnerId, String partnerName, BigDecimal amount) {
        log.info("Upsert partner {} with amount {} for amount {}", partnerId, amount, partnerName);
        em.createNativeQuery(
                        """
                        INSERT INTO partner_summary(partner_id, partner_name,
                                                    total_amount_transactions, total_count_transactions)
                        VALUES (:pid, :name, :amt, 1)
                        ON CONFLICT (partner_id)
                        DO UPDATE
                            SET total_amount_transactions = total_amount_transactions + :amt,
                                total_count_transactions    = total_count_transactions    + 1
                        """)
                .setParameter("pid", partnerId)
                .setParameter("name", partnerName)
                .setParameter("amt", amount)
                .executeUpdate();
    }



    public void processMtnCallback(MtnCallBackRequest mtnCallBackRequest) {

        //Log the callback received
        log.info("Processing MTN callback for externalId: {}", mtnCallBackRequest.externalId());

        //Build MtnCallback entity using builder pattern
        MtnCallback callback = MtnCallback.builder()
                .financialTransactionId(mtnCallBackRequest.financialTransactionId() != null ? mtnCallBackRequest.financialTransactionId() : "")
                .externalId(mtnCallBackRequest.externalId())
                .partyIdType(mtnCallBackRequest.payer().partyIdType())
                .partyId(mtnCallBackRequest.payer().partyId())
                .payerMessage(mtnCallBackRequest.payerMessage())
                .payeeNote(mtnCallBackRequest.payeeNote())
                .status(mtnCallBackRequest.status())
                .reason(mtnCallBackRequest.reason())
                .amount(mtnCallBackRequest.amount())
                .currency(mtnCallBackRequest.currency())
                .createdAt(LocalDateTime.now())
                .build();

        callbackRepository.save(callback);


        Optional<PaymentTransaction> transactionOpt = transactionRepository.findByExternalRef(mtnCallBackRequest.externalId());
        if (transactionOpt.isEmpty()) {
            log.warn("No transaction found for MTN callback with externalId: {}", mtnCallBackRequest.externalId());
            return;
        }

        PaymentTransaction transaction = transactionOpt.get();
        //If status is successful and reason is null or empty then it is a success
        if("SUCCESSFUL".equalsIgnoreCase(mtnCallBackRequest.status())
                && (mtnCallBackRequest.reason() == null || mtnCallBackRequest.reason().isEmpty())) {
            transaction.setStatus(TransactionStatus.SUCCESSFUL);
            transaction.setMessage("Payment successful");
            transaction.setCompletedAt(LocalDateTime.now());

            transaction.setMtnFinancialTransactionId(mtnCallBackRequest.financialTransactionId());
            transaction.setMtnExternalId(mtnCallBackRequest.externalId());
            transaction.setMtnPayerPartyIdType(mtnCallBackRequest.payer().partyIdType());
            transaction.setMtnPayerPartyId(mtnCallBackRequest.payer().partyId());
            transaction.setMtnPayerMessage(mtnCallBackRequest.payerMessage());
            transaction.setMtnPayeeNote(mtnCallBackRequest.payeeNote());

            Optional<String> partnerOpt = partnerSummaryRepository.findPartnerIdByNameIgnoreCase(transaction.getInitiationPartner());
            log.info("Fetching partner Id : {}", partnerOpt);
            if (partnerOpt.isPresent()) {
                BigDecimal amount = new BigDecimal(mtnCallBackRequest.amount());
                partnerSummaryRepository.upsertPartnerSummary(partnerOpt.get(), transaction.getInitiationPartner(), amount);
            }
        }  else {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setMessage(mtnCallBackRequest.reason() != null ? mtnCallBackRequest.reason() : "Payment failed");
            transaction.setCompletedAt(LocalDateTime.now());

            transaction.setMtnFinancialTransactionId(null);
            transaction.setMtnExternalId(mtnCallBackRequest.externalId());
            transaction.setMtnPayerPartyIdType(mtnCallBackRequest.payer().partyIdType());
            transaction.setMtnPayerPartyId(mtnCallBackRequest.payer().partyId());
            transaction.setMtnPayerMessage(mtnCallBackRequest.payerMessage());
            transaction.setMtnPayeeNote(mtnCallBackRequest.payeeNote());

        }

        transactionRepository.save(transaction);
        log.info("Processed MTN callback for transaction: {}, new status: {}", transaction.getTransactionRef(), transaction.getStatus());

    }

//    public PreApprovalResponse createPerApproval(String providerName, PreApprovalRequest preApprovalRequest) {
//        log.info("Creating pre-approval for provider: {}", providerName);
//        PaymentProvider provider = providerFactory.getProvider(providerName);
//        return provider.createPreApproval(preApprovalRequest);
//    }



    private String otpGenerator() {
        int otp = ThreadLocalRandom.current().nextInt(100000, 999999); // 6 digits
        return String.valueOf(otp);
    }

}
