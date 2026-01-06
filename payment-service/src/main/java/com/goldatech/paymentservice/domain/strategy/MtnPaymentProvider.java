package com.goldatech.paymentservice.domain.strategy;

import com.goldatech.paymentservice.domain.model.PaymentTransaction;
import com.goldatech.paymentservice.domain.model.TransactionStatus;
import com.goldatech.paymentservice.domain.model.UserRoles;
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

@Component("MTN")
@RequiredArgsConstructor
@Slf4j
public class MtnPaymentProvider implements PaymentProvider{
    private final PaymentTransactionRepository transactionRepository;
    private final PreApprovalTransactionRepository preApprovalTransactionRepository;
    private final SubscriptionRepository subscriptionRepository;
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

        BigDecimal netAmount = request.amount().subtract(finalCommissionAmount);

        RequestToPayRequest mtnRequest = new RequestToPayRequest(
                netAmount.toPlainString(),
                "GHS",
                referenceId,//externalId
                new Payer("MSISDN", request.mobileNumber()),
                orgName,//org Name used as payerMessage to aid in reconciliation
                request.payeeNote()
        );

        String xRef = mtnMomoService.requestToPay(mtnRequest, xReferenceId);


        // determine user role based on initiationPartner
        String initiationPartner = Optional.ofNullable(orgName).orElse("").trim();
        UserRoles userRole;
        if (initiationPartner.equalsIgnoreCase("Ferracore Technologies") || initiationPartner.equalsIgnoreCase("Rexhub")) {
            userRole = UserRoles.SUPER_ADMIN;
        } else if (initiationPartner.equalsIgnoreCase("Global Accelerex")) {
            userRole = UserRoles.GA_ADMIN;
        } else {
            userRole = UserRoles.BUSINESS_ADMIN;
        }



        // For now, we'll simulate a Pending transaction.

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
                .userRoles(userRole)
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


    public Optional<PreApprovalResponse> preApproval(PreApprovalRequest request) {
        log.info("Initiating pre-approval with MTN for mobile number: {}", request.payer().partyId());

        //Call the MTN Momo service to initiate pre-approval
        PreApprovalResponse preApprovalResponse = mtnMomoService.createPreApprovalMandate(request);
        log.info("Pre-approval response from MTN: {}", preApprovalResponse);

        return Optional.ofNullable(preApprovalResponse);

    }

    //check pre-approval status
    public Optional<PreApprovalStatusResponse> checkPreApprovalStatus(String mandateId) {
        log.info("Checking pre-approval status with MTN for mandate id: {}", mandateId);

        PreApprovalStatusResponse preApprovalStatusResponse = mtnMomoService.getPreApprovalStatus(mandateId);
        //Return optional
        return Optional.ofNullable(preApprovalStatusResponse);
    }

    //Cancel pre-approval
    public boolean cancelPreApproval(String mandateId) {
        log.info("Cancelling pre-approval with MTN for mandate id: {}", mandateId);

        return mtnMomoService.cancelPreapprovalMandate(mandateId);

    }
}
