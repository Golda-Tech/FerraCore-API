package com.goldatech.paymentservice.domain.model;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "momo_provider_config")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MomoConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 100)
    private TelcoProvider provider;

    @Column(name = "base_url")
    private String baseUrl;

    @Column(name = "collection_subscription_key")
    private String collectionSubscriptionKey;

    @Column(name = "disbursement_subscription_key")
    private String disbursementSubscriptionKey;

    @Column(name = "api_user")
    private String apiUser;

    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "environment")
    private String environment;

    @Column(name = "basic_auth_token", length = 2048)
    private String basicAuthToken;

}
