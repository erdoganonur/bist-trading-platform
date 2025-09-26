package com.bisttrading.gateway.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Redis-based Rate Limiting Service for BIST Trading Platform Gateway.
 *
 * Implements distributed rate limiting using Redis Lua scripts for atomicity.
 * Supports multiple rate limiting algorithms:
 * - Sliding Window with sub-windows
 * - Token Bucket with burst protection
 * - Fixed Window with gradual recovery
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisRateLimitingService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    private RedisScript<List> slidingWindowScript;
    private RedisScript<List> tokenBucketScript;
    private RedisScript<List> fixedWindowScript;

    @PostConstruct
    public void initializeScripts() {
        try {
            // Load Lua scripts
            slidingWindowScript = RedisScript.of(
                new ClassPathResource("lua/sliding-window-rate-limit.lua"),
                List.class
            );

            tokenBucketScript = RedisScript.of(
                new ClassPathResource("lua/token-bucket-rate-limit.lua"),
                List.class
            );

            fixedWindowScript = RedisScript.of(
                new ClassPathResource("lua/fixed-window-rate-limit.lua"),
                List.class
            );

            log.info("Rate limiting Lua scripts loaded successfully");

        } catch (Exception e) {
            log.error("Failed to load rate limiting Lua scripts", e);
            // Fallback to simple Redis operations if scripts fail to load
        }
    }

    /**
     * Check if request is allowed based on rate limit configuration.
     *
     * @param key Unique identifier for rate limiting (user ID, IP, etc.)
     * @param config Rate limit configuration
     * @return Rate limit result with decision and metadata
     */
    public Mono<RateLimitResult> isAllowed(String key, RateLimitConfig config) {
        if (!config.isValid()) {
            log.warn("Invalid rate limit config provided for key: {}", key);
            return Mono.just(RateLimitResult.error("Invalid rate limit configuration"));
        }

        String rateLimitKey = config.getKeyPrefix() + key;
        Instant now = Instant.now();

        if (config.isEnableSlidingWindow()) {
            return checkSlidingWindow(rateLimitKey, config, now);
        } else {
            return checkFixedWindow(rateLimitKey, config, now);
        }
    }

    /**
     * Sliding window rate limiting implementation.
     */
    private Mono<RateLimitResult> checkSlidingWindow(String key, RateLimitConfig config, Instant now) {
        if (slidingWindowScript == null) {
            return fallbackRateLimiting(key, config, now);
        }

        long currentTime = now.toEpochMilli();
        long windowSizeMs = config.getWindowSize().toMillis();
        long subWindowSizeMs = config.getSubWindowDuration().toMillis();

        List<String> keys = List.of(key);
        List<String> args = List.of(
            String.valueOf(currentTime),
            String.valueOf(windowSizeMs),
            String.valueOf(subWindowSizeMs),
            String.valueOf(config.getRequestsPerSecond()),
            String.valueOf(config.getBurstCapacity()),
            String.valueOf(config.getTokensPerRequest()),
            String.valueOf(config.getNumberOfSubWindows())
        );

        return redisTemplate.execute(slidingWindowScript, keys, args)
            .cast(List.class)
            .single()
            .map(result -> {
                if (result.size() < 4) {
                    return RateLimitResult.error("Invalid script response");
                }

                boolean allowed = "1".equals(String.valueOf(result.get(0)));
                long remainingTokens = Long.parseLong(String.valueOf(result.get(1)));
                long retryAfterMs = Long.parseLong(String.valueOf(result.get(2)));
                long totalRequests = Long.parseLong(String.valueOf(result.get(3)));

                return RateLimitResult.builder()
                    .allowed(allowed)
                    .remainingTokens(remainingTokens)
                    .retryAfter(Duration.ofMillis(retryAfterMs))
                    .totalRequests(totalRequests)
                    .algorithm("sliding-window")
                    .windowSize(config.getWindowSize())
                    .rateLimitKey(key)
                    .build();
            })
            .doOnError(error -> log.error("Sliding window rate limiting failed for key: " + key, error))
            .onErrorResume(error -> {
                log.error("Redis script error for key: " + key, error);
                return fallbackRateLimiting(key, config, now);
            });
    }

    /**
     * Fixed window rate limiting implementation.
     */
    private Mono<RateLimitResult> checkFixedWindow(String key, RateLimitConfig config, Instant now) {
        if (fixedWindowScript == null) {
            return fallbackRateLimiting(key, config, now);
        }

        long currentTime = now.toEpochMilli();
        long windowSizeMs = config.getWindowSize().toMillis();

        List<String> keys = List.of(key);
        List<String> args = List.of(
            String.valueOf(currentTime),
            String.valueOf(windowSizeMs),
            String.valueOf(config.getRequestsPerSecond()),
            String.valueOf(config.getBurstCapacity()),
            String.valueOf(config.getTokensPerRequest())
        );

        return redisTemplate.execute(fixedWindowScript, keys, args)
            .cast(List.class)
            .single()
            .map(result -> {
                if (result.size() < 4) {
                    return RateLimitResult.error("Invalid script response");
                }

                boolean allowed = "1".equals(String.valueOf(result.get(0)));
                long remainingTokens = Long.parseLong(String.valueOf(result.get(1)));
                long retryAfterMs = Long.parseLong(String.valueOf(result.get(2)));
                long totalRequests = Long.parseLong(String.valueOf(result.get(3)));

                return RateLimitResult.builder()
                    .allowed(allowed)
                    .remainingTokens(remainingTokens)
                    .retryAfter(Duration.ofMillis(retryAfterMs))
                    .totalRequests(totalRequests)
                    .algorithm("fixed-window")
                    .windowSize(config.getWindowSize())
                    .rateLimitKey(key)
                    .build();
            })
            .doOnError(error -> log.error("Fixed window rate limiting failed for key: " + key, error))
            .onErrorResume(error -> {
                log.error("Redis script error for key: " + key, error);
                return fallbackRateLimiting(key, config, now);
            });
    }

    /**
     * Token bucket rate limiting (alternative algorithm).
     */
    public Mono<RateLimitResult> checkTokenBucket(String key, RateLimitConfig config, Instant now) {
        if (tokenBucketScript == null) {
            return fallbackRateLimiting(key, config, now);
        }

        long currentTime = now.toEpochMilli();

        List<String> keys = List.of(key);
        List<String> args = List.of(
            String.valueOf(currentTime),
            String.valueOf(config.getRequestsPerSecond()),
            String.valueOf(config.getBurstCapacity()),
            String.valueOf(config.getTokensPerRequest())
        );

        return redisTemplate.execute(tokenBucketScript, keys, args)
            .cast(List.class)
            .single()
            .map(result -> {
                if (result.size() < 3) {
                    return RateLimitResult.error("Invalid script response");
                }

                boolean allowed = "1".equals(String.valueOf(result.get(0)));
                long remainingTokens = Long.parseLong(String.valueOf(result.get(1)));
                long retryAfterMs = Long.parseLong(String.valueOf(result.get(2)));

                return RateLimitResult.builder()
                    .allowed(allowed)
                    .remainingTokens(remainingTokens)
                    .retryAfter(Duration.ofMillis(retryAfterMs))
                    .algorithm("token-bucket")
                    .rateLimitKey(key)
                    .build();
            })
            .doOnError(error -> log.error("Token bucket rate limiting failed for key: " + key, error))
            .onErrorResume(error -> {
                log.error("Redis script error for key: " + key, error);
                return fallbackRateLimiting(key, config, now);
            });
    }

    /**
     * Fallback rate limiting using simple Redis operations.
     * Used when Lua scripts fail or are not available.
     */
    private Mono<RateLimitResult> fallbackRateLimiting(String key, RateLimitConfig config, Instant now) {
        log.debug("Using fallback rate limiting for key: {}", key);

        return redisTemplate.opsForValue()
            .get(key)
            .switchIfEmpty(Mono.just("0"))
            .map(Long::parseLong)
            .flatMap(currentCount -> {
                if (currentCount >= config.getBurstCapacity()) {
                    // Rate limit exceeded
                    return Mono.just(RateLimitResult.builder()
                        .allowed(false)
                        .remainingTokens(0)
                        .retryAfter(config.getPenaltyDuration())
                        .totalRequests(currentCount)
                        .algorithm("fallback-simple")
                        .rateLimitKey(key)
                        .build());
                }

                // Increment counter
                return redisTemplate.opsForValue()
                    .increment(key, config.getTokensPerRequest())
                    .flatMap(newCount -> {
                        // Set expiry if this is the first request
                        if (newCount == config.getTokensPerRequest()) {
                            return redisTemplate.expire(key, config.getWindowSize())
                                .thenReturn(newCount);
                        }
                        return Mono.just(newCount);
                    })
                    .map(newCount -> RateLimitResult.builder()
                        .allowed(true)
                        .remainingTokens(Math.max(0, config.getBurstCapacity() - newCount))
                        .retryAfter(Duration.ZERO)
                        .totalRequests(newCount)
                        .algorithm("fallback-simple")
                        .rateLimitKey(key)
                        .build());
            })
            .onErrorReturn(RateLimitResult.error("Fallback rate limiting failed"));
    }

    /**
     * Get current rate limit status without consuming tokens.
     */
    public Mono<RateLimitStatus> getStatus(String key, RateLimitConfig config) {
        String rateLimitKey = config.getKeyPrefix() + key;

        return redisTemplate.opsForValue()
            .get(rateLimitKey)
            .switchIfEmpty(Mono.just("0"))
            .map(Long::parseLong)
            .map(currentCount -> RateLimitStatus.builder()
                .rateLimitKey(rateLimitKey)
                .currentRequests(currentCount)
                .remainingCapacity(Math.max(0, config.getBurstCapacity() - currentCount))
                .resetTime(Instant.now().plus(config.getWindowSize()))
                .config(config)
                .build())
            .onErrorReturn(RateLimitStatus.error("Failed to get rate limit status"));
    }

    /**
     * Reset rate limit for a specific key (admin operation).
     */
    public Mono<Void> resetRateLimit(String key, RateLimitConfig config) {
        String rateLimitKey = config.getKeyPrefix() + key;

        return redisTemplate.delete(rateLimitKey)
            .doOnSuccess(deleted -> {
                if (deleted > 0) {
                    log.info("Rate limit reset for key: {}", rateLimitKey);
                } else {
                    log.debug("No rate limit data found to reset for key: {}", rateLimitKey);
                }
            })
            .doOnError(error -> log.error("Failed to reset rate limit for key: " + rateLimitKey, error))
            .then();
    }

    /**
     * Get rate limiting statistics for monitoring.
     */
    public Mono<RateLimitStatistics> getStatistics(String keyPattern) {
        return redisTemplate.keys(keyPattern + "*")
            .collectList()
            .flatMap(keys -> {
                if (keys.isEmpty()) {
                    return Mono.just(RateLimitStatistics.empty());
                }

                return redisTemplate.opsForValue()
                    .multiGet(keys)
                    .map(values -> {
                        int totalKeys = keys.size();
                        long totalRequests = values.stream()
                            .mapToLong(v -> v != null ? Long.parseLong(v) : 0)
                            .sum();

                        return RateLimitStatistics.builder()
                            .totalKeys(totalKeys)
                            .totalRequests(totalRequests)
                            .averageRequests(totalKeys > 0 ? (double) totalRequests / totalKeys : 0)
                            .keyPattern(keyPattern)
                            .build();
                    });
            })
            .onErrorReturn(RateLimitStatistics.error("Failed to get statistics"));
    }
}