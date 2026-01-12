package com.goldatech.paymentservice.web.controller;

import com.goldatech.paymentservice.domain.model.PaymentTransaction;
import com.goldatech.paymentservice.domain.service.PaymentService;
import com.goldatech.paymentservice.web.dto.request.MtnCallBackRequest;
import com.goldatech.paymentservice.web.dto.request.NameEnquiryRequest;
import com.goldatech.paymentservice.web.dto.request.PaymentRequest;
import com.goldatech.paymentservice.web.dto.response.NameEnquiryResponse;
import com.goldatech.paymentservice.web.dto.response.PaymentResponse;
import com.goldatech.paymentservice.web.dto.response.PaymentTrendDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<Object> initiatePayment(@Valid @RequestBody PaymentRequest request,
                                                  @RequestHeader("X-Callback-Url") String callbackUrl,
                                                   @RequestHeader("X-Reference-Id") String referenceId,
                                                  @RequestHeader("X-Target-Environment") String env) {
        log.info("Received payment initiation request for provider: {}", request.provider());

        try {
            PaymentResponse response = paymentService.initiatePayment(request, callbackUrl, referenceId, env);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Bad request while initiating payment: {}", e.getMessage(), e);
            Map<String, Object> body = new HashMap<>();
            body.put("error", "Bad Request");
            body.put("message", e.getMessage());
            body.put("type", e.getClass().getSimpleName());
            body.put("sampleStack", Arrays.stream(e.getStackTrace())
                    .limit(5)
                    .map(StackTraceElement::toString)
                    .collect(Collectors.joining("\n")));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        } catch (Exception e) {
            log.error("Unexpected error while initiating payment: {}", e.getMessage(), e);
            Map<String, Object> body = new HashMap<>();
            body.put("error", "Internal Server Error");
            body.put("message", e.getMessage());
            body.put("type", e.getClass().getSimpleName());
            body.put("sampleStack", Arrays.stream(e.getStackTrace())
                    .limit(10)
                    .map(StackTraceElement::toString)
                    .collect(Collectors.joining("\n")));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<PaymentResponse> checkPaymentStatus(@RequestParam String provider, @RequestParam String transactionRef) {
        log.info("Received status check request for transaction: {}", transactionRef);
        PaymentResponse response = paymentService.checkPaymentStatus(provider, transactionRef);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/name-enquiry")
    public ResponseEntity<NameEnquiryResponse> nameEnquiry(@Valid @RequestParam String mobileNumber) {
        NameEnquiryRequest request = new NameEnquiryRequest(mobileNumber);
        log.info("Received name enquiry request for mobile: {}", request.mobileNumber());
        return paymentService.nameEnquiry(request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    //Get all payments
    @GetMapping
    public ResponseEntity<Iterable<PaymentTransaction>> getAllPayments(@RequestParam String initiatedBy) {
        log.info("Received request to fetch all payments");
        Iterable<PaymentTransaction> payments = paymentService.getAllPayments(initiatedBy);
        return ResponseEntity.ok(payments);
    }

    //Get payment by transaction reference
    @GetMapping("/{transactionRef}")
    public ResponseEntity<PaymentTransaction> getPaymentByTransactionRef(@PathVariable String transactionRef) {
        log.info("Received request to fetch payment with transactionRef: {}", transactionRef);
        return paymentService.getPaymentByTransactionRef(transactionRef)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());

    }


    //OTP before payment
    @GetMapping("/send-otp")
    public ResponseEntity<String> sendOtp(@RequestParam String destination, @RequestParam String channel, @RequestParam String type) {
        log.info("Received request to send OTP to destination: {}", destination);
        boolean otpSent = paymentService.sendOtp(destination, channel, type);
        if (otpSent) {
            return ResponseEntity.ok("OTP sent successfully");
        } else {
            return ResponseEntity.status(500).body("Failed to send OTP");

        }

    }

    // Verify OTP
    @GetMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(@RequestParam String identifier, @RequestParam String otp, @RequestParam String channel) {
        log.info("Received request to verify OTP for identifier: {}", identifier);
        boolean otpValid = paymentService.verifyOtp(identifier, otp, channel);
        if (otpValid) {
            return ResponseEntity.ok("OTP verified successfully");
        } else {
            return ResponseEntity.status(400).body("Invalid OTP");
        }

    }

    @GetMapping("/status-summary")
    public ResponseEntity<Map<String, Long>> getCollectionStatusSummary(@RequestParam String initiatedBy) {
        try {
            return ResponseEntity.ok(paymentService.getPaymentStatusSummary(initiatedBy));
        } catch (Exception e) {
            log.error("Error getting collection status summary: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/trends")
    public ResponseEntity<List<PaymentTrendDTO>> getTrends(
            @RequestParam String initiatedBy,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "DAILY") PaymentService.Interval interval

    ) {
        try {
            List<PaymentTrendDTO> trends = paymentService.getPaymentTrends(initiatedBy, startDate, endDate, interval);
            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            log.error("Error fetching trends: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @RequestMapping(value = "/mtn/callback", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<String> handleMtnCallback(@RequestBody MtnCallBackRequest mtnCallBackRequest) {
        log.info("Received MTN callback for externalId: {}", mtnCallBackRequest.externalId());
        paymentService.processMtnCallback(mtnCallBackRequest);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("");
    }


}