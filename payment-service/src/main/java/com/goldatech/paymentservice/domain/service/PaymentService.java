package com.goldatech.paymentservice.domain.service;

import com.goldatech.paymentservice.domain.model.PaymentTransaction;
import com.goldatech.paymentservice.domain.repository.PaymentTransactionRepository;
import com.goldatech.paymentservice.domain.strategy.PaymentProvider;
import com.goldatech.paymentservice.domain.strategy.PaymentProviderFactory;
import com.goldatech.paymentservice.web.dto.request.NameEnquiryRequest;
import com.goldatech.paymentservice.web.dto.request.PaymentRequest;
import com.goldatech.paymentservice.web.dto.response.NameEnquiryResponse;
import com.goldatech.paymentservice.web.dto.response.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentProviderFactory providerFactory;
    private final PaymentTransactionRepository transactionRepository;

    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {
        log.info("Initiating payment request for provider: {}", request.provider());
        PaymentProvider provider = providerFactory.getProvider(request.provider());
        PaymentTransaction transaction = provider.initiatePayment(request);
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
