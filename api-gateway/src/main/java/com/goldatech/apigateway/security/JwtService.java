package com.goldatech.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    public Mono<TokenInfo> validateTokenAndExtractInfo(String token) {
        return Mono.fromCallable(() -> {
            try {
                Claims claims = extractAllClaims(token);

                if (isTokenExpired(claims)) {
                    throw new RuntimeException("Token expired");
                }

                TokenInfo tokenInfo = new TokenInfo();
                tokenInfo.setUsername(claims.getSubject());
                tokenInfo.setRoles(claims.get("roles", String.class));
                tokenInfo.setUserId(claims.get("userId", String.class));

                return tokenInfo;
            } catch (Exception e) {
                throw new RuntimeException("Invalid token: " + e.getMessage());
            }
        });
    }

    private boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Data
    public static class TokenInfo {
        private String username;
        private String roles;
        private String userId;
    }
}