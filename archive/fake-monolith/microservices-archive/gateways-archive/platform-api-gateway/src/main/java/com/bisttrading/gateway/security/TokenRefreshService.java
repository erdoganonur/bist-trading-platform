package com.bisttrading.gateway.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Token Refresh Service for BIST Trading Platform Gateway.
 *
 * Handles JWT token refresh operations including:
 * - Refresh token validation
 * - New access token generation
 * - Token rotation and security
 * - Refresh token blacklisting
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRefreshService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final WebClient.Builder webClientBuilder;

    private static final String REFRESH_TOKEN_PREFIX = "jwt:refresh:";
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    /**
     * Refresh access token using refresh token.
     *
     * @param refreshToken The refresh token
     * @param clientIp Client IP address for security tracking
     * @param userAgent User agent for device tracking
     * @return New token pair (access + refresh tokens)
     */
    public Mono<TokenRefreshResponse> refreshAccessToken(String refreshToken, String clientIp, String userAgent) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return Mono.just(TokenRefreshResponse.error("Refresh token is required"));
        }

        log.debug("Token refresh request from IP: {}", clientIp);

        return validateRefreshToken(refreshToken)
            .flatMap(isValid -> {
                if (!isValid) {
                    log.warn("Invalid refresh token used from IP: {}", clientIp);
                    return Mono.just(TokenRefreshResponse.error("Invalid or expired refresh token"));
                }

                return callUserServiceForTokenRefresh(refreshToken, clientIp, userAgent);
            })
            .doOnError(error -> log.error("Token refresh failed", error))
            .onErrorReturn(TokenRefreshResponse.error("Token refresh service unavailable"));
    }

    /**
     * Validate refresh token against Redis store.
     */
    private Mono<Boolean> validateRefreshToken(String refreshToken) {
        return isTokenBlacklisted(refreshToken)
            .map(isBlacklisted -> !isBlacklisted)
            .onErrorReturn(false);
    }

    /**
     * Check if refresh token is blacklisted.
     */
    private Mono<Boolean> isTokenBlacklisted(String refreshToken) {
        try {
            // Extract token ID from refresh token for blacklist checking
            // In a real implementation, you would parse the JWT to get the JTI claim
            String tokenId = extractTokenId(refreshToken);
            String blacklistKey = BLACKLIST_PREFIX + tokenId;

            return redisTemplate.hasKey(blacklistKey)
                .onErrorReturn(false);
        } catch (Exception e) {
            log.warn("Failed to check refresh token blacklist status", e);
            return Mono.just(false);
        }
    }

    /**
     * Call user management service to refresh token.
     */
    private Mono<TokenRefreshResponse> callUserServiceForTokenRefresh(
            String refreshToken, String clientIp, String userAgent) {

        WebClient webClient = webClientBuilder
            .baseUrl("http://localhost:8081")
            .build();

        Map<String, Object> refreshRequest = Map.of(
            "refreshToken", refreshToken,
            "clientIp", clientIp,
            "userAgent", userAgent
        );

        return webClient.post()
            .uri("/api/v1/auth/refresh")
            .header("X-Gateway-Request", "true")
            .header("X-Client-IP", clientIp)
            .header("User-Agent", userAgent)
            .bodyValue(refreshRequest)
            .retrieve()
            .bodyToMono(Map.class)
            .map(response -> {
                String newAccessToken = (String) response.get("accessToken");
                String newRefreshToken = (String) response.get("refreshToken");
                Integer expiresIn = (Integer) response.get("expiresIn");

                if (newAccessToken == null || newRefreshToken == null) {
                    return TokenRefreshResponse.error("Invalid response from auth service");
                }

                log.info("Token refreshed successfully for client IP: {}", clientIp);

                return TokenRefreshResponse.success(newAccessToken, newRefreshToken, expiresIn);
            })
            .onErrorResume(error -> {
                log.error("Failed to refresh token from user service", error);
                return Mono.just(TokenRefreshResponse.error("Authentication service unavailable"));
            });
    }

    /**
     * Blacklist refresh token (e.g., during logout).
     */
    public Mono<Void> blacklistRefreshToken(String refreshToken, String reason) {
        try {
            String tokenId = extractTokenId(refreshToken);
            String blacklistKey = BLACKLIST_PREFIX + tokenId;

            // Set TTL based on token expiry (fallback to 24 hours)
            Duration ttl = Duration.ofHours(24);

            String blacklistData = String.format(
                "{\"reason\":\"%s\",\"blacklistedAt\":\"%s\",\"type\":\"refresh\"}",
                reason, Instant.now()
            );

            return redisTemplate.opsForValue()
                .set(blacklistKey, blacklistData, ttl)
                .then()
                .doOnSuccess(v -> log.info("Refresh token blacklisted: reason={}", reason))
                .doOnError(e -> log.error("Failed to blacklist refresh token", e));

        } catch (Exception e) {
            log.error("Error blacklisting refresh token", e);
            return Mono.empty();
        }
    }

    /**
     * Extract token ID from JWT token (simplified version).
     * In a real implementation, this would parse the JWT and extract the JTI claim.
     */
    private String extractTokenId(String token) {
        // This is a simplified implementation
        // In reality, you would parse the JWT and extract the 'jti' claim
        return "token-id-" + token.hashCode();
    }

    /**
     * Token refresh response container.
     */
    public static class TokenRefreshResponse {
        private final boolean success;
        private final String accessToken;
        private final String refreshToken;
        private final Integer expiresIn;
        private final String errorMessage;

        private TokenRefreshResponse(boolean success, String accessToken, String refreshToken,
                                   Integer expiresIn, String errorMessage) {
            this.success = success;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresIn = expiresIn;
            this.errorMessage = errorMessage;
        }

        public static TokenRefreshResponse success(String accessToken, String refreshToken, Integer expiresIn) {
            return new TokenRefreshResponse(true, accessToken, refreshToken, expiresIn, null);
        }

        public static TokenRefreshResponse error(String errorMessage) {
            return new TokenRefreshResponse(false, null, null, null, errorMessage);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public Integer getExpiresIn() { return expiresIn; }
        public String getErrorMessage() { return errorMessage; }
    }
}