package com.goldatech.paymentservice.web.controller;

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
    public ResponseEntity<PaymentResponse> initiatePayment(@Valid @RequestBody PaymentRequest request) {
        log.info("Received payment initiation request for provider: {}", request.provider());
        PaymentResponse response = paymentService.initiatePayment(request);
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
}