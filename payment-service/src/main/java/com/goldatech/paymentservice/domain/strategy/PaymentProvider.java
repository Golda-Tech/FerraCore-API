package com.goldatech.paymentservice.domain.strategy;

import com.goldatech.paymentservice.domain.model.PaymentTransaction;
import com.goldatech.paymentservice.web.dto.request.NameEnquiryRequest;
import com.goldatech.paymentservice.web.dto.request.PaymentRequest;
import com.goldatech.paymentservice.web.dto.response.NameEnquiryResponse;

import java.util.Optional;

public interface PaymentProvider {
    /**
     * Initiates a payment transaction with a third-party provider.
     * @param request The payment request details.
     * @return The updated PaymentTransaction model after initiating.
     */
    PaymentTransaction initiatePayment(PaymentRequest request, String callbackUrl, String referenceId);

    /**
     * Checks the status of a payment transaction with a third-party provider.
     * @param transactionRef The internal transaction reference.
     * @return The updated PaymentTransaction model with the latest status.
     */
    PaymentTransaction checkStatus(String transactionRef);

    /**
     * Performs a name enquiry to verify account details with a third-party provider.
     * @param request The name enquiry request details.
     * @return The response containing account name and status.
     */
    Optional<NameEnquiryResponse> nameEnquiry(NameEnquiryRequest request);

}
