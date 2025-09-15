package com.goldatech.paymentservice.domain.service;

import com.goldatech.paymentservice.domain.model.momo.MtnToken;
import com.goldatech.paymentservice.domain.repository.MtnTokenRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class MtnTokenScheduler {

    private final MtnMomoService mtnMomoService;
    private final MtnTokenRepository tokenRepository;

    /**
     * Run every 45 minutes:
            */
    @Scheduled(cron = "0 */45 * * * *")
    public void refreshTokens() {
        log.info("Starting scheduled MTN MoMo token refresh...");

        try {
            var collectionToken = mtnMomoService.getCollectionToken();
            saveToken("COLLECTION", collectionToken.accessToken(), collectionToken.expiresIn());

            var disbursementToken = mtnMomoService.getDisbursementToken();
            saveToken("DISBURSEMENT", disbursementToken.accessToken(), disbursementToken.expiresIn());

            log.info("MTN MoMo tokens refreshed successfully.");
        } catch (Exception e) {
            log.error("Error refreshing MTN MoMo tokens: {}", e.getMessage(), e);
        }
    }

    private void saveToken(String type, String accessToken, Integer expiresIn) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusSeconds(expiresIn != null ? expiresIn : 3600);

        MtnToken token = tokenRepository.findTopByTypeOrderByCreatedAtDesc(type)
                .orElse(MtnToken.builder().type(type).build());

        token.setAccessToken(accessToken);
        token.setCreatedAt(now);
        token.setExpiresAt(expiresAt);

        tokenRepository.save(token);
        log.debug("Stored {} token valid until {}", type, expiresAt);
    }


    @PostConstruct
    public void init() {
        refreshTokens(); // run once immediately on startup
    }
}