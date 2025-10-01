package com.bisttrading.core.security.service;

import com.bisttrading.core.security.test.TestDataBuilder;
import com.bisttrading.core.security.config.JwtProperties;
import com.bisttrading.core.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TokenBlacklistService.
 * Tests token blacklisting and validation scenarios.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Token Blacklist Service Tests")
class TokenBlacklistServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtProperties jwtProperties;

    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        tokenBlacklistService = new TokenBlacklistService(redisTemplate, jwtTokenProvider, jwtProperties);
    }

    @Test
    @DisplayName("Should blacklist token successfully")
    void shouldBlacklistTokenSuccessfully() {
        // Given
        String token = TestDataBuilder.JwtTokens.VALID_ACCESS_TOKEN;
        String jwtId = "test-jwt-id";
        when(jwtTokenProvider.getJwtIdFromToken(token)).thenReturn(jwtId);

        // When
        tokenBlacklistService.blacklistToken(token, "Test blacklist");

        // Then
        verify(jwtTokenProvider).getJwtIdFromToken(token);
        verify(valueOperations).set(
            contains("blacklist"),
            eq("Test blacklist"),
            any(Duration.class)
        );
    }

    @Test
    @DisplayName("Should blacklist token with Turkish characters")
    void shouldBlacklistTokenWithTurkishCharacters() {
        // Given
        String tokenWithTurkishChars = "türkçe-karakter-içeren-token-ğüşöçı";
        String jwtId = "türkçe-jwt-id";
        when(jwtTokenProvider.getJwtIdFromToken(tokenWithTurkishChars)).thenReturn(jwtId);

        // When
        tokenBlacklistService.blacklistToken(tokenWithTurkishChars, "Türkçe sebep");

        // Then
        verify(jwtTokenProvider).getJwtIdFromToken(tokenWithTurkishChars);
        verify(valueOperations).set(
            contains("blacklist"),
            eq("Türkçe sebep"),
            any(Duration.class)
        );
    }

    @Test
    @DisplayName("Should handle null token gracefully")
    void shouldHandleNullTokenGracefully() {
        // When & Then
        assertThatNoException()
            .isThrownBy(() -> tokenBlacklistService.blacklistToken(null, "Null token"));

        // Verify no Redis operations were performed
        verifyNoInteractions(valueOperations);
    }

    @Test
    @DisplayName("Should handle empty token gracefully")
    void shouldHandleEmptyTokenGracefully() {
        // When & Then
        assertThatNoException()
            .isThrownBy(() -> tokenBlacklistService.blacklistToken("", "Empty token"));

        // Verify no Redis operations were performed
        verifyNoInteractions(valueOperations);
    }

    @Test
    @DisplayName("Should check if token is blacklisted")
    void shouldCheckIfTokenIsBlacklisted() {
        // Given
        String token = TestDataBuilder.JwtTokens.VALID_ACCESS_TOKEN;
        String jwtId = "test-jwt-id";
        when(jwtTokenProvider.getJwtIdFromToken(token)).thenReturn(jwtId);
        when(valueOperations.get(any())).thenReturn("BLACKLISTED");

        // When
        boolean isBlacklisted = tokenBlacklistService.isTokenBlacklisted(token);

        // Then
        assertThat(isBlacklisted).isTrue();
        verify(jwtTokenProvider).getJwtIdFromToken(token);
        verify(valueOperations).get(any());
    }

    @Test
    @DisplayName("Should return false for non-blacklisted token")
    void shouldReturnFalseForNonBlacklistedToken() {
        // Given
        String token = TestDataBuilder.JwtTokens.VALID_ACCESS_TOKEN;
        String jwtId = "test-jwt-id";
        when(jwtTokenProvider.getJwtIdFromToken(token)).thenReturn(jwtId);
        when(valueOperations.get(any())).thenReturn(null);

        // When
        boolean isBlacklisted = tokenBlacklistService.isTokenBlacklisted(token);

        // Then
        assertThat(isBlacklisted).isFalse();
        verify(jwtTokenProvider).getJwtIdFromToken(token);
        verify(valueOperations).get(any());
    }
}