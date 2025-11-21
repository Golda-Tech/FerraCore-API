package com.goldatech.paymentservice.domain.model;

import com.goldatech.paymentservice.web.dto.request.Payer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "mtn_callbacks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MtnCallback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String externalId;
    private String amount;
    private String currency;
    private String partyIdType;
    private String partyId;
    private String payerMessage;
    private String payeeNote;
    private String status;
    private String reason;
    private String financialTransactionId;
    private LocalDateTime createdAt;
}
