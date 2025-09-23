package com.goldatech.paymentservice.web.controller;

import com.goldatech.paymentservice.domain.model.PaymentTransaction;
import com.goldatech.paymentservice.domain.service.PaymentService;
import com.goldatech.paymentservice.web.dto.request.NameEnquiryRequest;
import com.goldatech.paymentservice.web.dto.request.PaymentRequest;
import com.goldatech.paymentservice.web.dto.response.NameEnquiryResponse;
import com.goldatech.paymentservice.web.dto.response.PaymentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> initiatePayment(@Valid @RequestBody PaymentRequest request,
                                                           @RequestHeader("X-User-Email") String email,
                                                           @RequestHeader("X-User-Id") String userId) {
        log.info("Received payment initiation request for provider: {}", request.provider());
        log.info("Request initiated by user: {} with email: {}", userId, email);
        PaymentResponse response = paymentService.initiatePayment(request, userId, email);
        return ResponseEntity.ok(response);
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


    //Get a;ll payments
    @GetMapping
    public ResponseEntity<Iterable<PaymentTransaction>> getAllPayments() {
        log.info("Received request to fetch all payments");
        Iterable<PaymentTransaction> payments = paymentService.getAllPayments();
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

}