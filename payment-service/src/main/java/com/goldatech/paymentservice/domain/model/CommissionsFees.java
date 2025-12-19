package com.goldatech.paymentservice.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "commissions_fees")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionsFees {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String transactionFee;
    private String cappedAmount;
}