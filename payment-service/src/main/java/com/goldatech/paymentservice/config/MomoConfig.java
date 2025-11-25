package com.goldatech.paymentservice.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.goldatech.paymentservice.domain.model.MomoConfigEntity;
import com.goldatech.paymentservice.domain.model.MomoProperties;
import com.goldatech.paymentservice.domain.model.TelcoProvider;
import com.goldatech.paymentservice.domain.service.MomoConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;


@Configuration
@Slf4j
public class MomoConfig {
    @Bean
    public Map<TelcoProvider, MomoProperties> momoPropertiesByProvider(MomoConfigService svc, Environment env) {
        Map<TelcoProvider, MomoProperties> result = new HashMap<>();
        List<MomoConfigEntity> rows = svc.loadAllConfigs();
        log.info("Loaded Momo Config rows from DB: {} ", rows);

        for (MomoConfigEntity row : rows) {
            TelcoProvider provider = row.getProvider();
            if (provider == null) continue; // skip malformed rows

            String providerKey = provider.name().toLowerCase(); // used for env lookup: momo.{provider}.{key}

            MomoProperties props = new MomoProperties();
            props.setBaseUrl(firstNonNull(row.getBaseUrl(),
                    env.getProperty("momo." + providerKey + ".base-url",
                            env.getProperty("momo.defaults.base-url", "<default-url>"))));

            props.setCollectionSubscriptionKey(firstNonNull(row.getCollectionSubscriptionKey(),
                    env.getProperty("momo." + providerKey + ".collection-subscription-key",
                            env.getProperty("momo.defaults.collection-subscription-key", "<default-collections-sub-key>"))));

            props.setDisbursementSubscriptionKey(firstNonNull(row.getDisbursementSubscriptionKey(),
                    env.getProperty("momo." + providerKey + ".disbursement-subscription-key",
                            env.getProperty("momo.defaults.disbursement-subscription-key", "<default-disbursement-sub-key>"))));

            props.setApiUser(firstNonNull(row.getApiUser(),
                    env.getProperty("momo." + providerKey + ".api-user",
                            env.getProperty("momo.defaults.api-user", "PLACEHOLDER"))));

            props.setApiKey(firstNonNull(row.getApiKey(),
                    env.getProperty("momo." + providerKey + ".api-key",
                            env.getProperty("momo.defaults.api-key", "PLACEHOLDER"))));

            props.setEnvironment(firstNonNull(row.getEnvironment(),
                    env.getProperty("momo." + providerKey + ".environment",
                            env.getProperty("momo.defaults.environment", "<default-environment>"))));

            props.setBasicAuthToken(firstNonNull(row.getBasicAuthToken(),
                    env.getProperty("momo." + providerKey + ".basic-auth-token",
                            env.getProperty("momo.defaults.basic-auth-token", "<default-token>"))));

            result.put(provider, props);
        }
        log.info("Config details: {} ", result);

        return result;
    }

    private String firstNonNull(String primary, String fallback) {
        return (primary != null && !primary.isBlank()) ? primary : fallback;
    }
}

