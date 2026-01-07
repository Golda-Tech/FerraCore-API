package com.goldatech.paymentservice.web.controller;

import com.goldatech.paymentservice.domain.model.MandateStatus;
import com.goldatech.paymentservice.domain.service.PreApprovalService;
import com.goldatech.paymentservice.web.dto.request.PreApprovalMandateRequest;
import com.goldatech.paymentservice.web.dto.response.PreApprovalCancelResponse;
import com.goldatech.paymentservice.web.dto.response.PreApprovalMandateResponse;
import com.goldatech.paymentservice.web.dto.response.momo.PreApprovalResponse;
import com.goldatech.paymentservice.web.dto.response.momo.PreApprovalStatusResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/mandates")
@RequiredArgsConstructor
@Slf4j
public class PreApprovalController {

    private final PreApprovalService preApprovalService;

    @PostMapping
    public ResponseEntity<PreApprovalResponse> createPreApproval(@Valid @RequestBody PreApprovalMandateRequest request){
        log.info("Received pre-approval mandate creation request for provider: {}", request.provider());
        PreApprovalResponse response = preApprovalService.createPreApprovalMandate(request);
        return ResponseEntity.ok(response);
    }

    // Check Pre-Approval Status Endpoint
    @GetMapping("/{mandateId}/status")
    public ResponseEntity<PreApprovalStatusResponse> getPreApprovalStatus(@PathVariable String mandateId){
        log.info("Received pre-approval status request for mandate ID: {}", mandateId);
        PreApprovalStatusResponse response = preApprovalService.checkPreApprovalStatus( "MTN", mandateId);
        return ResponseEntity.ok(response);
    }



    //cancel Pre-Approval Endpoint
    @PostMapping("/{mandateId}/cancel")
    public ResponseEntity<PreApprovalCancelResponse> cancelPreApproval(@PathVariable String mandateId) {
        log.info("Received pre-approval cancellation request for mandate ID: {}", mandateId);
        PreApprovalCancelResponse response = preApprovalService.cancelPreApprovalMandate("MTN", mandateId);
        return ResponseEntity.ok(response);
    }

}
