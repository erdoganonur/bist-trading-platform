package com.bisttrading.core.security.service;

import com.bisttrading.core.security.config.JwtProperties;
import com.bisttrading.core.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Token blacklist service using Redis for JWT token invalidation.
 * Manages blacklisted tokens to prevent reuse after logout or security events.
 */
@Slf4j
@Service
public class TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    public TokenBlacklistService(@Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate,
                                JwtTokenProvider jwtTokenProvider,
                                JwtProperties jwtProperties) {
        this.redisTemplate = redisTemplate;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
    }

    private static final String BLACKLIST_PREFIX = "bist:security:blacklist:";
    private static final String USER_TOKENS_PREFIX = "bist:security:user-tokens:";
    private static final String CLEANUP_LOCK = "bist:security:cleanup-lock";

    /**
     * Adds a token to the blacklist.
     *
     * @param token JWT token to blacklist
     * @param reason Reason for blacklisting
     */
    public void blacklistToken(String token, String reason) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("Boş token blacklist'e eklenemez");
            return;
        }

        try {
            String jwtId = jwtTokenProvider.getJwtIdFromToken(token);
            if (jwtId == null) {
                log.warn("Token'dan JWT ID alınamadı, token blacklist'e eklenemedi");
                return;
            }

            String key = BLACKLIST_PREFIX + jwtId;
            long ttl = jwtTokenProvider.getTimeUntilExpiry(token);

            if (ttl <= 0) {
                log.debug("Süresi dolmuş token blacklist'e eklenmedi - jwtId: {}", jwtId);
                return;
            }

            // Store blacklist entry with token expiry as TTL
            BlacklistEntry entry = new BlacklistEntry(
                jwtId,
                reason,
                LocalDateTime.now(),
                jwtTokenProvider.getExpiryFromToken(token)
            );

            redisTemplate.opsForValue().set(key, entry.toString(), ttl, TimeUnit.SECONDS);

            log.info("Token blacklist'e eklendi - jwtId: {}, reason: {}, ttl: {}s",
                jwtId, reason, ttl);

        } catch (Exception e) {
            log.error("Token blacklist'e eklenirken hata - error: {}", e.getMessage(), e);
        }
    }

    /**
     * Checks if a token is blacklisted.
     *
     * @param token JWT token to check
     * @return true if token is blacklisted
     */
    public boolean isTokenBlacklisted(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        try {
            String jwtId = jwtTokenProvider.getJwtIdFromToken(token);
            if (jwtId == null) {
                return false;
            }

            String key = BLACKLIST_PREFIX + jwtId;
            Boolean exists = redisTemplate.hasKey(key);

            log.debug("Token blacklist kontrolü - jwtId: {}, blacklisted: {}",
                jwtId, exists != null && exists);

            return exists != null && exists;

        } catch (Exception e) {
            log.error("Token blacklist kontrolü hatası - error: {}", e.getMessage());
            // In case of error, assume token is valid to avoid blocking legitimate users
            return false;
        }
    }

    /**
     * Blacklists all active tokens for a user (useful for logout from all devices).
     *
     * @param userId User ID
     * @param reason Reason for blacklisting
     */
    public void blacklistAllUserTokens(String userId, String reason) {
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }

        try {
            String userTokensKey = USER_TOKENS_PREFIX + userId;
            Set<String> userTokens = redisTemplate.opsForSet().members(userTokensKey);

            if (userTokens != null && !userTokens.isEmpty()) {
                for (String jwtId : userTokens) {
                    String blacklistKey = BLACKLIST_PREFIX + jwtId;
                    BlacklistEntry entry = new BlacklistEntry(
                        jwtId,
                        reason,
                        LocalDateTime.now(),
                        LocalDateTime.now().plus(jwtProperties.getAccessTokenExpiry())
                    );

                    redisTemplate.opsForValue().set(blacklistKey, entry.toString(),
                        jwtProperties.getAccessTokenExpiryInSeconds(), TimeUnit.SECONDS);
                }

                // Clear user tokens set
                redisTemplate.delete(userTokensKey);

                log.info("Kullanıcının tüm token'ları blacklist'e eklendi - userId: {}, count: {}, reason: {}",
                    userId, userTokens.size(), reason);
            }

        } catch (Exception e) {
            log.error("Kullanıcının token'ları blacklist'e eklenirken hata - userId: {}, error: {}",
                userId, e.getMessage(), e);
        }
    }

    /**
     * Tracks a token for a user (used to enable logout from all devices).
     *
     * @param userId User ID
     * @param token JWT token
     */
    public void trackUserToken(String userId, String token) {
        if (userId == null || token == null) {
            return;
        }

        try {
            String jwtId = jwtTokenProvider.getJwtIdFromToken(token);
            if (jwtId == null) {
                return;
            }

            String userTokensKey = USER_TOKENS_PREFIX + userId;
            long ttl = jwtTokenProvider.getTimeUntilExpiry(token);

            if (ttl > 0) {
                redisTemplate.opsForSet().add(userTokensKey, jwtId);
                redisTemplate.expire(userTokensKey, Duration.ofSeconds(ttl));

                log.debug("Kullanıcı token'ı takip ediliyor - userId: {}, jwtId: {}",
                    userId, jwtId);
            }

        } catch (Exception e) {
            log.error("Kullanıcı token'ı takip edilirken hata - userId: {}, error: {}",
                userId, e.getMessage());
        }
    }

    /**
     * Removes a token from user tracking.
     *
     * @param userId User ID
     * @param token JWT token
     */
    public void untrackUserToken(String userId, String token) {
        if (userId == null || token == null) {
            return;
        }

        try {
            String jwtId = jwtTokenProvider.getJwtIdFromToken(token);
            if (jwtId == null) {
                return;
            }

            String userTokensKey = USER_TOKENS_PREFIX + userId;
            redisTemplate.opsForSet().remove(userTokensKey, jwtId);

            log.debug("Kullanıcı token'ı takipten çıkarıldı - userId: {}, jwtId: {}",
                userId, jwtId);

        } catch (Exception e) {
            log.error("Kullanıcı token'ı takipten çıkarılırken hata - userId: {}, error: {}",
                userId, e.getMessage());
        }
    }

    /**
     * Gets the blacklist reason for a token.
     *
     * @param token JWT token
     * @return Blacklist reason or null if not blacklisted
     */
    public String getBlacklistReason(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        try {
            String jwtId = jwtTokenProvider.getJwtIdFromToken(token);
            if (jwtId == null) {
                return null;
            }

            String key = BLACKLIST_PREFIX + jwtId;
            String entryData = redisTemplate.opsForValue().get(key);

            if (entryData != null) {
                // Parse the reason from the stored entry
                // Simple parsing assuming format: "jwtId:reason:timestamp:expiry"
                String[] parts = entryData.split(":", 4);
                if (parts.length >= 2) {
                    return parts[1];
                }
            }

            return null;

        } catch (Exception e) {
            log.error("Blacklist sebebi alınırken hata - error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Gets the count of blacklisted tokens.
     *
     * @return Number of blacklisted tokens
     */
    public long getBlacklistedTokenCount() {
        try {
            Set<String> keys = redisTemplate.keys(BLACKLIST_PREFIX + "*");
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            log.error("Blacklist sayısı alınırken hata - error: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Clears expired blacklist entries (runs periodically).
     */
    @Scheduled(fixedDelayString = "#{@jwtProperties.blacklistCleanupInterval.toMillis()}")
    public void cleanupExpiredTokens() {
        if (!jwtProperties.isEnableBlacklist()) {
            return;
        }

        try {
            // Try to acquire cleanup lock
            Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(CLEANUP_LOCK, "1", Duration.ofMinutes(5));

            if (lockAcquired == null || !lockAcquired) {
                log.debug("Blacklist temizlik işlemi zaten çalışıyor, atlanıyor");
                return;
            }

            log.info("Blacklist temizlik işlemi başlatılıyor");
            long startTime = System.currentTimeMillis();

            Set<String> blacklistKeys = redisTemplate.keys(BLACKLIST_PREFIX + "*");
            if (blacklistKeys == null || blacklistKeys.isEmpty()) {
                log.debug("Temizlenecek blacklist kaydı bulunamadı");
                return;
            }

            int cleanedCount = 0;
            for (String key : blacklistKeys) {
                // Redis TTL will automatically remove expired keys,
                // but we can do additional cleanup if needed
                Long ttl = redisTemplate.getExpire(key);
                if (ttl != null && ttl < 0) {
                    redisTemplate.delete(key);
                    cleanedCount++;
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Blacklist temizlik işlemi tamamlandı - temizlenen: {}, süre: {}ms",
                cleanedCount, duration);

        } catch (Exception e) {
            log.error("Blacklist temizlik işlemi hatası - error: {}", e.getMessage(), e);
        } finally {
            // Release cleanup lock
            try {
                redisTemplate.delete(CLEANUP_LOCK);
            } catch (Exception e) {
                log.warn("Cleanup lock serbest bırakılamadı - error: {}", e.getMessage());
            }
        }
    }

    /**
     * Clears all blacklist entries (admin operation).
     */
    public void clearAllBlacklistedTokens() {
        try {
            Set<String> blacklistKeys = redisTemplate.keys(BLACKLIST_PREFIX + "*");
            if (blacklistKeys != null && !blacklistKeys.isEmpty()) {
                redisTemplate.delete(blacklistKeys);
                log.warn("Tüm blacklist kayıtları temizlendi - count: {}", blacklistKeys.size());
            }

            Set<String> userTokenKeys = redisTemplate.keys(USER_TOKENS_PREFIX + "*");
            if (userTokenKeys != null && !userTokenKeys.isEmpty()) {
                redisTemplate.delete(userTokenKeys);
                log.warn("Tüm kullanıcı token takip kayıtları temizlendi - count: {}", userTokenKeys.size());
            }

        } catch (Exception e) {
            log.error("Blacklist temizliği hatası - error: {}", e.getMessage(), e);
        }
    }

    /**
     * Checks if blacklist functionality is enabled.
     *
     * @return true if enabled
     */
    public boolean isBlacklistEnabled() {
        return jwtProperties.isEnableBlacklist();
    }

    /**
     * Blacklist entry data structure.
     */
    private static class BlacklistEntry {
        private final String jwtId;
        private final String reason;
        private final LocalDateTime blacklistedAt;
        private final LocalDateTime expiresAt;

        public BlacklistEntry(String jwtId, String reason, LocalDateTime blacklistedAt, LocalDateTime expiresAt) {
            this.jwtId = jwtId;
            this.reason = reason;
            this.blacklistedAt = blacklistedAt;
            this.expiresAt = expiresAt;
        }

        @Override
        public String toString() {
            return String.format("%s:%s:%s:%s",
                jwtId != null ? jwtId : "",
                reason != null ? reason : "",
                blacklistedAt != null ? blacklistedAt.toString() : "",
                expiresAt != null ? expiresAt.toString() : "");
        }
    }
}