package com.goldatech.paymentservice.domain.model;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MomoProperties {
    // getters and setters
    private String provider;
    private String baseUrl;
    private String collectionSubscriptionKey;
    private String disbursementSubscriptionKey;
    private String apiUser;
    private String apiKey;
    private String environment;
    private String basicAuthToken;

}
