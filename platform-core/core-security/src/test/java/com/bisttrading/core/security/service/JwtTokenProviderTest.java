package com.bisttrading.core.security.service;

import com.bisttrading.core.security.config.JwtProperties;
import com.bisttrading.core.security.jwt.JwtTokenProvider;
import com.bisttrading.core.security.test.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JwtTokenProvider.
 * Tests JWT token generation, validation, and claim extraction with Turkish character support.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Token Provider Tests")
class JwtTokenProviderTest {

    @Mock
    private JwtProperties jwtProperties;

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        // Configure JWT properties mock
        when(jwtProperties.getSecret()).thenReturn("test-secret-key-for-jwt-token-generation-minimum-256-bits");
        when(jwtProperties.getIssuer()).thenReturn("bist-trading-platform");
        when(jwtProperties.getAudience()).thenReturn("bist-trading-users");
        when(jwtProperties.getAccessTokenExpiry()).thenReturn(Duration.ofMinutes(15));
        when(jwtProperties.getRefreshTokenExpiry()).thenReturn(Duration.ofDays(7));
        when(jwtProperties.getClockSkewInSeconds()).thenReturn(60);

        // Mock JwtProperties.Claims
        JwtProperties.Claims claims = mock(JwtProperties.Claims.class);
        when(claims.isIncludeRoles()).thenReturn(true);
        when(claims.isIncludeProfile()).thenReturn(true);
        when(jwtProperties.getClaims()).thenReturn(claims);

        // Mock validation
        doNothing().when(jwtProperties).validate();

        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
        jwtTokenProvider.init();
    }

    @Test
    @DisplayName("Should generate valid access token")
    void shouldGenerateValidAccessToken() {
        // Given
        CustomUserDetails userDetails = TestDataBuilder.createTestUserDetails();

        // When
        String token = jwtTokenProvider.generateAccessToken(userDetails);

        // Then
        assertThat(token).isNotNull().isNotEmpty();
        assertThat(jwtTokenProvider.isValidToken(token)).isTrue();
        assertThat(jwtTokenProvider.isAccessToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(userDetails.getUserId());
        assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo(userDetails.getEmail());
    }

    @Test
    @DisplayName("Should generate valid refresh token")
    void shouldGenerateValidRefreshToken() {
        // Given
        CustomUserDetails userDetails = TestDataBuilder.createTestUserDetails();

        // When
        String token = jwtTokenProvider.generateRefreshToken(userDetails);

        // Then
        assertThat(token).isNotNull().isNotEmpty();
        assertThat(jwtTokenProvider.isValidToken(token)).isTrue();
        assertThat(jwtTokenProvider.isRefreshToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(userDetails.getUserId());
    }

    @Test
    @DisplayName("Should handle Turkish characters correctly")
    void shouldHandleTurkishCharactersCorrectly() {
        // Given
        CustomUserDetails turkishUser = TestDataBuilder.createTurkishUserDetails();

        // When
        String token = jwtTokenProvider.generateAccessToken(turkishUser);

        // Then
        assertThat(token).isNotNull();
        assertThat(jwtTokenProvider.isValidToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(turkishUser.getUserId());
        assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo(turkishUser.getEmail());
    }

    @Test
    @DisplayName("Should validate token expiry correctly")
    void shouldValidateTokenExpiryCorrectly() {
        // Given
        CustomUserDetails userDetails = TestDataBuilder.createTestUserDetails();
        String token = jwtTokenProvider.generateAccessToken(userDetails);

        // When & Then
        assertThat(jwtTokenProvider.isTokenExpired(token)).isFalse();
        assertThat(jwtTokenProvider.getTimeUntilExpiry(token)).isPositive();
    }

    @Test
    @DisplayName("Should extract JWT ID from token")
    void shouldExtractJwtIdFromToken() {
        // Given
        CustomUserDetails userDetails = TestDataBuilder.createTestUserDetails();
        String token = jwtTokenProvider.generateAccessToken(userDetails);

        // When
        String jwtId = jwtTokenProvider.getJwtIdFromToken(token);

        // Then
        assertThat(jwtId).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("Should reject invalid tokens")
    void shouldRejectInvalidTokens() {
        // When & Then
        assertThat(jwtTokenProvider.isValidToken("invalid.token.format")).isFalse();
        assertThat(jwtTokenProvider.isValidToken(null)).isFalse();
        assertThat(jwtTokenProvider.isValidToken("")).isFalse();
    }

    @Test
    @DisplayName("Should handle malformed tokens gracefully")
    void shouldHandleMalformedTokensGracefully() {
        // Given
        String malformedToken = "not.a.valid.jwt";

        // When & Then
        assertThat(jwtTokenProvider.isValidToken(malformedToken)).isFalse();

        assertThatThrownBy(() -> jwtTokenProvider.getUserIdFromToken(malformedToken))
            .isInstanceOf(RuntimeException.class);
    }
}