package com.goldatech.paymentservice.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.goldatech.paymentservice.domain.model.MomoConfigEntity;
import com.goldatech.paymentservice.domain.model.MomoProperties;
import com.goldatech.paymentservice.domain.model.TelcoProvider;
import com.goldatech.paymentservice.domain.service.MomoConfigService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;


@Configuration
public class MomoConfig {
    @Bean
    public Map<TelcoProvider, MomoProperties> momoPropertiesByProvider(MomoConfigService svc, Environment env) {
        Map<TelcoProvider, MomoProperties> result = new HashMap<>();
        List<MomoConfigEntity> rows = svc.loadAllConfigs();

        for (MomoConfigEntity row : rows) {
            TelcoProvider provider = row.getProvider();
            if (provider == null) continue; // skip malformed rows

            String providerKey = provider.name().toLowerCase(); // used for env lookup: momo.{provider}.{key}

            MomoProperties props = new MomoProperties();
            props.setBaseUrl(firstNonNull(row.getBaseUrl(),
                    env.getProperty("momo." + providerKey + ".base-url",
                            env.getProperty("momo.defaults.base-url", "https://proxy.momoapi.MTN.com"))));

            props.setCollectionSubscriptionKey(firstNonNull(row.getCollectionSubscriptionKey(),
                    env.getProperty("momo." + providerKey + ".collection-subscription-key",
                            env.getProperty("momo.defaults.collection-subscription-key", "d79ac4e645854c3aa80b53f08e512eff"))));

            props.setDisbursementSubscriptionKey(firstNonNull(row.getDisbursementSubscriptionKey(),
                    env.getProperty("momo." + providerKey + ".disbursement-subscription-key",
                            env.getProperty("momo.defaults.disbursement-subscription-key", "9db1d12c4c524dd69d710652d8eff65b"))));

            props.setApiUser(firstNonNull(row.getApiUser(),
                    env.getProperty("momo." + providerKey + ".api-user",
                            env.getProperty("momo.defaults.api-user", "PLACEHOLDER"))));

            props.setApiKey(firstNonNull(row.getApiKey(),
                    env.getProperty("momo." + providerKey + ".api-key",
                            env.getProperty("momo.defaults.api-key", "PLACEHOLDER"))));

            props.setEnvironment(firstNonNull(row.getEnvironment(),
                    env.getProperty("momo." + providerKey + ".environment",
                            env.getProperty("momo.defaults.environment", "mtnghana"))));

            props.setBasicAuthToken(firstNonNull(row.getBasicAuthToken(),
                    env.getProperty("momo." + providerKey + ".basic-auth-token",
                            env.getProperty("momo.defaults.basic-auth-token", "<default-token>"))));

            result.put(provider, props);
        }

        return result;
    }

    private String firstNonNull(String primary, String fallback) {
        return (primary != null && !primary.isBlank()) ? primary : fallback;
    }
}

