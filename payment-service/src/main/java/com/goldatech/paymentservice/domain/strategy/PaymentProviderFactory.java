package com.goldatech.paymentservice.domain.strategy;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PaymentProviderFactory {

    private final Map<String, PaymentProvider> providers = new ConcurrentHashMap<>();

    public PaymentProviderFactory(Map<String, PaymentProvider> providers) {
        this.providers.putAll(providers);
    }

    public PaymentProvider getProvider(String providerName) {
        PaymentProvider provider = providers.get(providerName.toUpperCase());
        if (provider == null) {
            throw new IllegalArgumentException("Unknown payment provider: " + providerName);
        }
        return provider;
    }
}
