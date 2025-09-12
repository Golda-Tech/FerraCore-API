package com.goldatech.paymentservice.domain.strategy;

import com.goldatech.paymentservice.domain.model.PaymentTransaction;
import com.goldatech.paymentservice.domain.model.TransactionStatus;
import com.goldatech.paymentservice.domain.repository.PaymentTransactionRepository;
import com.goldatech.paymentservice.web.dto.request.NameEnquiryRequest;
import com.goldatech.paymentservice.web.dto.request.PaymentRequest;
import com.goldatech.paymentservice.web.dto.response.NameEnquiryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component("MTN")
@RequiredArgsConstructor
@Slf4j
public class MtnPaymentProvider implements PaymentProvider{
    private final PaymentTransactionRepository transactionRepository;

    @Override
    public PaymentTransaction initiatePayment(PaymentRequest request) {
        log.info("Initiating payment with MTN for mobile number: {}", request.mobileNumber());
        // In a real scenario, this is where you would call the MTN API.
        // For now, we'll simulate a successful transaction.
        String externalRef = "mtn-" + UUID.randomUUID().toString();
        PaymentTransaction transaction = PaymentTransaction.builder()
                .collectionRef(request.collectionRef())
                .transactionRef(UUID.randomUUID().toString())
                .externalRef(externalRef)
                .provider("MTN")
                .mobileNumber(request.mobileNumber())
                .amount(request.amount())
                .currency(request.currency())
                .status(TransactionStatus.PENDING)
                .message("Payment request sent to MTN.")
                .initiatedAt(LocalDateTime.now())
                .build();
        return transactionRepository.save(transaction);
    }

    @Override
    public PaymentTransaction checkStatus(String transactionRef) {
        log.info("Checking status with MTN for internal ref: {}", transactionRef);
        PaymentTransaction transaction = transactionRepository.findByTransactionRef(transactionRef)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found."));

        // Simulate a response from the MTN API.
        log.info("Simulating MTN status check. For a real system, a call to the MTN API would go here.");
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setMessage("Payment successful.");
        transaction.setCompletedAt(LocalDateTime.now());

        return transactionRepository.save(transaction);
    }

    @Override
    public Optional<NameEnquiryResponse> nameEnquiry(NameEnquiryRequest request) {
        log.info("Performing name enquiry for MTN mobile number: {}", request.mobileNumber());

        // Simulate a call to the MTN name enquiry API.
        if (request.mobileNumber().startsWith("024")) {
            return Optional.of(NameEnquiryResponse.builder()
                    .mobileNumber(request.mobileNumber())
                    .accountName("John Doe")
                    .message("Account name found.")
                    .build());
        }
        return Optional.empty();
    }
}
