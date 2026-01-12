package com.goldatech.authservice.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * JPA Entity for a user.
 * This class represents the 'users' table in the database and also
 * implements UserDetails for Spring Security integration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "partner_summary")
public class PartnerSummary  {

    @Id
    @GeneratedValue
    private Integer id;


    @Column(name = "partner_id")
    private String partnerId;

    @Column(name = "partner_name")
    private String partnerName;

    @Column(name = "total_amount_transactions")
    private BigDecimal totalAmountTransactions;

    @Column(name = "total_count_transactions")
    private Integer totalCountTransactions;

    @Column(name = "createdBy")
    private String createdBy;

}

