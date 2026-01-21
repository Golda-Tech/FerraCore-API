package com.goldatech.paymentservice.domain.strategy;

import com.goldatech.paymentservice.domain.model.PaymentLedger;
import com.goldatech.paymentservice.domain.model.PaymentTransaction;
import com.goldatech.paymentservice.domain.model.TransactionStatus;
import com.goldatech.paymentservice.domain.model.UserRoles;
import com.goldatech.paymentservice.domain.repository.PaymentLedgerRepository;
import com.goldatech.paymentservice.domain.repository.PaymentTransactionRepository;
import com.goldatech.paymentservice.domain.repository.PreApprovalTransactionRepository;
import com.goldatech.paymentservice.domain.repository.SubscriptionRepository;
import com.goldatech.paymentservice.domain.service.MtnMomoService;
import com.goldatech.paymentservice.util.ReferenceIdGenerator;
import com.goldatech.paymentservice.web.dto.request.NameEnquiryRequest;
import com.goldatech.paymentservice.web.dto.request.PaymentRequest;
import com.goldatech.paymentservice.web.dto.request.momo.Payer;
import com.goldatech.paymentservice.web.dto.request.momo.PreApprovalRequest;
import com.goldatech.paymentservice.web.dto.request.momo.RequestToPayRequest;
import com.goldatech.paymentservice.web.dto.response.NameEnquiryResponse;
import com.goldatech.paymentservice.web.dto.response.PreApprovalCancelResponse;
import com.goldatech.paymentservice.web.dto.response.momo.BasicUserInfoResponse;
import com.goldatech.paymentservice.web.dto.response.momo.PreApprovalResponse;
import com.goldatech.paymentservice.web.dto.response.momo.PreApprovalStatusResponse;
import com.goldatech.paymentservice.web.dto.response.momo.RequestToPayStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component("MTN")
@RequiredArgsConstructor
@Slf4j
public class MtnPaymentProvider implements PaymentProvider{
    private final PaymentTransactionRepository transactionRepository;
    private final PreApprovalTransactionRepository preApprovalTransactionRepository;
    private final SubscriptionRepository subscriptionRepository;
    public final PaymentLedgerRepository paymentLedgerRepository;
    private final MtnMomoService mtnMomoService;

    @Override
    public PaymentTransaction initiatePayment(PaymentRequest request, String callbackUrl, String xReferenceId) {
        log.info("Initiating payment for mobile number: {}", request.mobileNumber());
        //Call the MTN Momo service to initiate payment

        String orgName = subscriptionRepository.findOrganizationNameByContactEmail(request.initiatedBy())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No subscription found for partner [%s]".formatted(request.initiationPartnerId())
                ));

        //Check if subscription is active
        boolean isActive = subscriptionRepository.isSubscriptionActiveForOrganization(orgName);
        if (!isActive) {
            throw new IllegalArgumentException(
                    "Subscription for organization [%s] is not active.".formatted(orgName)
            );
        }

        // Generate reference id using injected generator. Fall back to "FC" if collectionRef is null.

        String referenceId = ReferenceIdGenerator.generate(
                request.collectionRef() != null ? request.collectionRef() : "FC"
        );

        // compute commission: 1.75% of the amount, capped at 35 GHS
        BigDecimal finalCommissionAmount = deriveCommissionFees(request);

        BigDecimal providerFee = deriveProviderFees(request.amount());

        BigDecimal netAmount = request.amount().subtract(finalCommissionAmount);

        RequestToPayRequest mtnRequest = new RequestToPayRequest(
                request.amount().toPlainString(),
                "GHS",
                referenceId,//externalId
                new Payer("MSISDN", request.mobileNumber()),
                orgName,//org Name used as payerMessage to aid in reconciliation
                request.payeeNote()
        );

        String xRef = mtnMomoService.requestToPay(mtnRequest, callbackUrl, xReferenceId);




        PaymentLedger ledger = PaymentLedger.builder()
                .partnerName(orgName)
                .transactionId(UUID.fromString(xRef))//Use the internal reference as transaction ID
                .transAmount(netAmount)//Amount after deducting commission
                .gatewayFee(finalCommissionAmount)//Fee paid to Ferracore Tech, capped at GH₵ 35.00
                .billingAmount(request.amount())//assumption that customer is billed the full amount including charges
                .providerFee(providerFee)//Fee paid to MTN, capped at GH₵ 20.00
                .settle0Mtn(request.amount())//Net from Provider
                .settle1Partner(netAmount)//Net to Partner
                .status(TransactionStatus.PENDING)
                .build();
        log.info("Saving payment ledger: {}", ledger);
        paymentLedgerRepository.save(ledger);



        PaymentTransaction transaction = PaymentTransaction.builder()
                .collectionRef(request.collectionRef())
                .transactionRef(xRef)
                .externalRef(referenceId)
                .provider("MTN")
                .mobileNumber(request.mobileNumber())
                .amount(netAmount)
                .transactionFee(finalCommissionAmount)
                .initiatedBy(request.initiatedBy())
                .initiationPartner(orgName)
                .userRoles(null)
                .currency("GHS")
                .status(TransactionStatus.PENDING)
                .message("Payment request sent to MTN.")
                .initiatedAt(LocalDateTime.now())
                .mtnFinancialTransactionId(null) // This would be set upon confirmation from MTN
                .mtnExternalId(null)
                .mtnPayerPartyIdType("MSISDN")
                .mtnPayerPartyId(request.mobileNumber())
                .mtnPayerMessage(request.payerMessage())
                .mtnPayeeNote(request.payeeNote())
                .build();
        return transactionRepository.save(transaction);
    }

    private static BigDecimal deriveCommissionFees(PaymentRequest request) {
        BigDecimal commissionFee = request.amount()
                .multiply(new BigDecimal("0.0175"))
                .setScale(8, RoundingMode.HALF_UP);

        BigDecimal cappedFee = commissionFee.min(new BigDecimal("35.00"))
                .setScale(8, RoundingMode.HALF_UP);

        if (commissionFee.compareTo(cappedFee) > 0) {
            commissionFee = cappedFee;
        }

        //round commission fee to 2dp
        return commissionFee.setScale(2, RoundingMode.HALF_UP);
    }


    private static BigDecimal deriveProviderFees(BigDecimal amount) {
        BigDecimal providerFee = amount
                .multiply(new BigDecimal("0.01"))
                .setScale(8, RoundingMode.HALF_UP);

        BigDecimal cappedFee = providerFee.min(new BigDecimal("20.00"))
                .setScale(8, RoundingMode.HALF_UP);

        if (providerFee.compareTo(cappedFee) > 0) {
            providerFee = cappedFee;
        }

        //round provider fee to 2dp
        return providerFee.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public PaymentTransaction checkStatus(String transactionRef) {
        log.info("Checking status with MTN for internal ref: {}", transactionRef);
        PaymentTransaction transaction = transactionRepository.findByTransactionRef(transactionRef)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found."));

        // Simulate a response from the MTN API.
        log.info("Simulating MTN status check. For a real system, a call to the MTN API would go here.");
        //Call the MTN Momo service to check payment status
        RequestToPayStatusResponse statusResponse = mtnMomoService.getRequestToPayStatus(transactionRef);

        transaction.setStatus(TransactionStatus.valueOf(statusResponse.status()));
        transaction.setMessage(statusResponse.reason());
        if (transaction.getStatus() == TransactionStatus.SUCCESSFUL || transaction.getStatus() == TransactionStatus.FAILED) {
            transaction.setCompletedAt(LocalDateTime.now());
        }
        transaction.setMtnFinancialTransactionId(statusResponse.financialTransactionId());
        transaction.setMtnExternalId(statusResponse.externalId());

        return transactionRepository.save(transaction);
    }

    @Override
    public Optional<NameEnquiryResponse> nameEnquiry(NameEnquiryRequest request) {
        log.info("Performing name enquiry for MTN mobile number: {}", request.mobileNumber());

        // Simulate a call to the MTN name enquiry API.
        if (!request.mobileNumber().startsWith("233")) {
            log.warn("Invalid mobile number format for MTN: {}", request.mobileNumber());
            return Optional.empty();
        }

        BasicUserInfoResponse userInfo = mtnMomoService.getBasicUserInfo(request.mobileNumber());
        if (userInfo != null && userInfo.firstName() != null && userInfo.lastName() != null) {
            String accountName = userInfo.firstName() + " " + userInfo.lastName();
            return Optional.of(NameEnquiryResponse.builder()
                    .mobileNumber(request.mobileNumber())
                    .accountName(accountName)
                    .message("Account name found.")
                    .build());
        }

        return Optional.empty();
    }


    public PreApprovalResponse preApproval(PreApprovalRequest request) {
        log.info("Initiating pre-approval with MTN for mobile number: {}", request.payer().partyId());

        //Call the MTN Momo service to initiate pre-approval
        PreApprovalResponse preApprovalResponse = mtnMomoService.createPreApprovalMandate(request);
        log.info("Pre-approval response from MTN: {}", preApprovalResponse);

        return preApprovalResponse;
    }

    //check pre-approval status
    public PreApprovalStatusResponse checkPreApprovalStatus(String mandateId) {
        log.info("Checking pre-approval status with MTN for mandate id: {}", mandateId);

        return (PreApprovalStatusResponse) mtnMomoService.getPreApprovalStatus(mandateId);
    }

    //Cancel pre-approval
    public PreApprovalCancelResponse cancelPreApproval(String mandateId) {
        log.info("Cancelling pre-approval with MTN for mandate id: {}", mandateId);

        return mtnMomoService.cancelPreapprovalMandate(mandateId);

    }
}
