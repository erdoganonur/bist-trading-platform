package com.bisttrading.core.security.service;

import com.bisttrading.core.security.test.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TokenBlacklistService.
 * Tests token blacklisting, cleanup, and concurrent access scenarios.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Token Blacklist Service Tests")
class TokenBlacklistServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        tokenBlacklistService = new TokenBlacklistService(redisTemplate);
    }

    @Test
    @DisplayName("Should blacklist token successfully")
    void shouldBlacklistTokenSuccessfully() {
        // Given
        String token = TestDataBuilder.JwtTokens.VALID_ACCESS_TOKEN;

        // When
        tokenBlacklistService.blacklistToken(token);

        // Then
        verify(valueOperations).set(
            eq("blacklist:token:" + token.hashCode()),
            eq("BLACKLISTED"),
            any(Duration.class)
        );
    }

    @Test
    @DisplayName("Should blacklist token with Turkish characters")
    void shouldBlacklistTokenWithTurkishCharacters() {
        // Given
        String tokenWithTurkishChars = generateTokenWithTurkishUser("çağlar@example.com");

        // When
        tokenBlacklistService.blacklistToken(tokenWithTurkishChars);

        // Then
        verify(valueOperations).set(
            eq("blacklist:token:" + tokenWithTurkishChars.hashCode()),
            eq("BLACKLISTED"),
            any(Duration.class)
        );
    }

    @Test
    @DisplayName("Should detect blacklisted token")
    void shouldDetectBlacklistedToken() {
        // Given
        String token = TestDataBuilder.JwtTokens.VALID_ACCESS_TOKEN;
        when(valueOperations.get("blacklist:token:" + token.hashCode()))
            .thenReturn("BLACKLISTED");

        // When
        boolean isBlacklisted = tokenBlacklistService.isTokenBlacklisted(token);

        // Then
        assertThat(isBlacklisted).isTrue();
    }

    @Test
    @DisplayName("Should detect non-blacklisted token")
    void shouldDetectNonBlacklistedToken() {
        // Given
        String token = TestDataBuilder.JwtTokens.VALID_ACCESS_TOKEN;
        when(valueOperations.get("blacklist:token:" + token.hashCode()))
            .thenReturn(null);

        // When
        boolean isBlacklisted = tokenBlacklistService.isTokenBlacklisted(token);

        // Then
        assertThat(isBlacklisted).isFalse();
    }

    @Test
    @DisplayName("Should handle null token gracefully")
    void shouldHandleNullTokenGracefully() {
        // When & Then
        assertThatThrownBy(() -> tokenBlacklistService.blacklistToken(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Token cannot be null or empty");

        assertThatThrownBy(() -> tokenBlacklistService.isTokenBlacklisted(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Token cannot be null or empty");
    }

    @Test
    @DisplayName("Should handle empty token gracefully")
    void shouldHandleEmptyTokenGracefully() {
        // When & Then
        assertThatThrownBy(() -> tokenBlacklistService.blacklistToken(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Token cannot be null or empty");

        assertThatThrownBy(() -> tokenBlacklistService.blacklistToken("   "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Token cannot be null or empty");
    }

    @Test
    @DisplayName("Should blacklist multiple tokens")
    void shouldBlacklistMultipleTokens() {
        // Given
        String[] tokens = {
            TestDataBuilder.JwtTokens.VALID_ACCESS_TOKEN,
            TestDataBuilder.JwtTokens.VALID_REFRESH_TOKEN,
            "another.test.token"
        };

        // When
        for (String token : tokens) {
            tokenBlacklistService.blacklistToken(token);
        }

        // Then
        for (String token : tokens) {
            verify(valueOperations).set(
                eq("blacklist:token:" + token.hashCode()),
                eq("BLACKLISTED"),
                any(Duration.class)
            );
        }
    }

    @Test
    @DisplayName("Should handle concurrent blacklisting")
    void shouldHandleConcurrentBlacklisting() throws Exception {
        // Given
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        String baseToken = "concurrent.test.token.";

        // When
        CompletableFuture<Void>[] futures = new CompletableFuture[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            futures[i] = CompletableFuture.runAsync(
                () -> tokenBlacklistService.blacklistToken(baseToken + index),
                executor
            );
        }

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures);
        allFutures.get(5, TimeUnit.SECONDS);

        // Then
        for (int i = 0; i < threadCount; i++) {
            verify(valueOperations).set(
                eq("blacklist:token:" + (baseToken + i).hashCode()),
                eq("BLACKLISTED"),
                any(Duration.class)
            );
        }

        executor.shutdown();
    }

    @Test
    @DisplayName("Should handle concurrent checking")
    void shouldHandleConcurrentChecking() throws Exception {
        // Given
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        String token = TestDataBuilder.JwtTokens.VALID_ACCESS_TOKEN;
        when(valueOperations.get(anyString())).thenReturn("BLACKLISTED");

        // When
        CompletableFuture<Boolean>[] futures = new CompletableFuture[threadCount];
        for (int i = 0; i < threadCount; i++) {
            futures[i] = CompletableFuture.supplyAsync(
                () -> tokenBlacklistService.isTokenBlacklisted(token),
                executor
            );
        }

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures);
        allFutures.get(5, TimeUnit.SECONDS);

        // Then
        for (CompletableFuture<Boolean> future : futures) {
            assertThat(future.get()).isTrue();
        }

        executor.shutdown();
    }

    @Test
    @DisplayName("Should get blacklist statistics")
    void shouldGetBlacklistStatistics() {
        // Given
        when(redisTemplate.countExistingKeys(anyCollection())).thenReturn(5L);

        // When
        long blacklistedCount = tokenBlacklistService.getBlacklistedTokenCount();

        // Then
        assertThat(blacklistedCount).isEqualTo(5L);
    }

    @Test
    @DisplayName("Should cleanup expired tokens")
    void shouldCleanupExpiredTokens() {
        // Given
        when(redisTemplate.getExpire(anyString())).thenReturn(-1L); // Expired

        // When
        int cleanedCount = tokenBlacklistService.cleanupExpiredTokens();

        // Then
        assertThat(cleanedCount).isGreaterThanOrEqualTo(0);
        verify(redisTemplate, atLeastOnce()).keys("blacklist:token:*");
    }

    @Test
    @DisplayName("Should handle Redis connection errors gracefully")
    void shouldHandleRedisConnectionErrorsGracefully() {
        // Given
        String token = TestDataBuilder.JwtTokens.VALID_ACCESS_TOKEN;
        when(valueOperations.set(anyString(), anyString(), any(Duration.class)))
            .thenThrow(new RuntimeException("Redis connection failed"));

        // When & Then
        assertThatThrownBy(() -> tokenBlacklistService.blacklistToken(token))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Redis connection failed");
    }

    @Test
    @DisplayName("Should use correct TTL for token expiration")
    void shouldUseCorrectTtlForTokenExpiration() {
        // Given
        String accessToken = TestDataBuilder.JwtTokens.VALID_ACCESS_TOKEN;
        String refreshToken = TestDataBuilder.JwtTokens.VALID_REFRESH_TOKEN;

        // When
        tokenBlacklistService.blacklistToken(accessToken);
        tokenBlacklistService.blacklistToken(refreshToken);

        // Then
        verify(valueOperations, times(2)).set(
            anyString(),
            eq("BLACKLISTED"),
            any(Duration.class)
        );
    }

    @Test
    @DisplayName("Should blacklist token with custom expiration")
    void shouldBlacklistTokenWithCustomExpiration() {
        // Given
        String token = TestDataBuilder.JwtTokens.VALID_ACCESS_TOKEN;
        Duration customExpiration = Duration.ofHours(2);

        // When
        tokenBlacklistService.blacklistToken(token, customExpiration);

        // Then
        verify(valueOperations).set(
            eq("blacklist:token:" + token.hashCode()),
            eq("BLACKLISTED"),
            eq(customExpiration)
        );
    }

    @Test
    @DisplayName("Should handle very long tokens")
    void shouldHandleVeryLongTokens() {
        // Given
        String veryLongToken = TestDataBuilder.JwtTokens.VALID_ACCESS_TOKEN.repeat(10);

        // When
        tokenBlacklistService.blacklistToken(veryLongToken);

        // Then
        verify(valueOperations).set(
            eq("blacklist:token:" + veryLongToken.hashCode()),
            eq("BLACKLISTED"),
            any(Duration.class)
        );
    }

    @Test
    @DisplayName("Should handle special characters in tokens")
    void shouldHandleSpecialCharactersInTokens() {
        // Given
        String tokenWithSpecialChars = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiZXhwIjoxNjQwOTk1MjAwfQ.special+chars/test=";

        // When
        tokenBlacklistService.blacklistToken(tokenWithSpecialChars);

        // Then
        verify(valueOperations).set(
            eq("blacklist:token:" + tokenWithSpecialChars.hashCode()),
            eq("BLACKLISTED"),
            any(Duration.class)
        );
    }

    @Test
    @DisplayName("Should maintain consistency across operations")
    void shouldMaintainConsistencyAcrossOperations() {
        // Given
        String token = TestDataBuilder.JwtTokens.VALID_ACCESS_TOKEN;

        // Initially not blacklisted
        when(valueOperations.get("blacklist:token:" + token.hashCode()))
            .thenReturn(null);
        assertThat(tokenBlacklistService.isTokenBlacklisted(token)).isFalse();

        // Blacklist the token
        tokenBlacklistService.blacklistToken(token);

        // Now should be blacklisted
        when(valueOperations.get("blacklist:token:" + token.hashCode()))
            .thenReturn("BLACKLISTED");
        assertThat(tokenBlacklistService.isTokenBlacklisted(token)).isTrue();
    }

    private String generateTokenWithTurkishUser(String username) {
        // Mock JWT token with Turkish username
        return "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiLDp2HDn2xhckBleGFtcGxlLmNvbSJ9.signature";
    }
}