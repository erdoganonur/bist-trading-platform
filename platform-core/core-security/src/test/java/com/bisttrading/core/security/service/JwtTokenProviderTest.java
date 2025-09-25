package com.bisttrading.core.security.service;

import com.bisttrading.core.security.config.JwtProperties;
import com.bisttrading.core.security.jwt.JwtTokenProvider;
import com.bisttrading.core.security.test.TestDataBuilder;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JwtTokenProvider.
 * Tests JWT token generation, validation, and Turkish character handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JWT Token Provider Tests")
class JwtTokenProviderTest {

    @Mock
    private JwtProperties jwtProperties;

    private JwtTokenProvider jwtTokenProvider;
    private String testPrivateKey;
    private String testPublicKey;

    @BeforeEach
    void setUp() {
        // Generate test RSA key pair for testing
        testPrivateKey = generateTestPrivateKey();
        testPublicKey = generateTestPublicKey();

        // Mock JWT properties
        when(jwtProperties.getPrivateKeyLocation()).thenReturn("classpath:test-keys/jwt-private.pem");
        when(jwtProperties.getPublicKeyLocation()).thenReturn("classpath:test-keys/jwt-public.pem");
        when(jwtProperties.getAccessTokenExpiration()).thenReturn(Duration.ofMinutes(15));
        when(jwtProperties.getRefreshTokenExpiration()).thenReturn(Duration.ofDays(7));
        when(jwtProperties.getIssuer()).thenReturn("bist-trading-platform-test");
        when(jwtProperties.getAudience()).thenReturn("bist-trading-users-test");

        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
    }

    @Test
    @DisplayName("Should generate valid access token for regular username")
    void shouldGenerateValidAccessToken() {
        // Given
        String username = "test@example.com";

        // When
        String token = jwtTokenProvider.generateToken(username);

        // Then
        assertThat(token).isNotNull().isNotEmpty();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUsernameFromToken(token)).isEqualTo(username);
    }

    @Test
    @DisplayName("Should generate valid access token for Turkish username")
    void shouldGenerateValidAccessTokenForTurkishUsername() {
        // Given
        String turkishUsername = "çağlar@example.com";

        // When
        String token = jwtTokenProvider.generateToken(turkishUsername);

        // Then
        assertThat(token).isNotNull().isNotEmpty();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUsernameFromToken(token)).isEqualTo(turkishUsername);
    }

    @ParameterizedTest
    @DisplayName("Should handle various Turkish characters in username")
    @ValueSource(strings = {
        "çağlar@example.com",
        "gülşah@example.com",
        "ömer@example.com",
        "şeyma@example.com",
        "ibrahim@example.com",
        "ğülizar@example.com"
    })
    void shouldHandleTurkishCharactersInUsername(String turkishUsername) {
        // When
        String token = jwtTokenProvider.generateToken(turkishUsername);

        // Then
        assertThat(token).isNotNull().isNotEmpty();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUsernameFromToken(token)).isEqualTo(turkishUsername);
    }

    @Test
    @DisplayName("Should generate valid refresh token")
    void shouldGenerateValidRefreshToken() {
        // Given
        String username = "test@example.com";

        // When
        String refreshToken = jwtTokenProvider.generateRefreshToken(username);

        // Then
        assertThat(refreshToken).isNotNull().isNotEmpty();
        assertThat(jwtTokenProvider.validateToken(refreshToken)).isTrue();
        assertThat(jwtTokenProvider.getUsernameFromToken(refreshToken)).isEqualTo(username);

        // Verify it's marked as refresh token
        Claims claims = jwtTokenProvider.getAllClaimsFromToken(refreshToken);
        assertThat(claims.get("type")).isEqualTo("refresh");
    }

    @Test
    @DisplayName("Should set correct expiration times")
    void shouldSetCorrectExpirationTimes() {
        // Given
        String username = "test@example.com";
        Instant beforeGeneration = Instant.now();

        // When
        String accessToken = jwtTokenProvider.generateToken(username);
        String refreshToken = jwtTokenProvider.generateRefreshToken(username);
        Instant afterGeneration = Instant.now();

        // Then
        Date accessExpiration = jwtTokenProvider.getExpirationDateFromToken(accessToken);
        Date refreshExpiration = jwtTokenProvider.getExpirationDateFromToken(refreshToken);

        // Access token should expire in ~15 minutes
        Instant expectedAccessExpiration = beforeGeneration.plus(Duration.ofMinutes(15));
        assertThat(accessExpiration.toInstant())
            .isAfter(expectedAccessExpiration.minusSeconds(10))
            .isBefore(afterGeneration.plus(Duration.ofMinutes(15)).plusSeconds(10));

        // Refresh token should expire in ~7 days
        Instant expectedRefreshExpiration = beforeGeneration.plus(Duration.ofDays(7));
        assertThat(refreshExpiration.toInstant())
            .isAfter(expectedRefreshExpiration.minusSeconds(10))
            .isBefore(afterGeneration.plus(Duration.ofDays(7)).plusSeconds(10));
    }

    @Test
    @DisplayName("Should validate token correctly")
    void shouldValidateTokenCorrectly() {
        // Given
        String username = "test@example.com";
        String validToken = jwtTokenProvider.generateToken(username);

        // When & Then
        assertThat(jwtTokenProvider.validateToken(validToken)).isTrue();
        assertThat(jwtTokenProvider.validateToken("invalid.token.here")).isFalse();
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
        assertThat(jwtTokenProvider.validateToken(null)).isFalse();
    }

    @Test
    @DisplayName("Should extract username from token correctly")
    void shouldExtractUsernameFromTokenCorrectly() {
        // Given
        String username = "çağlar@türkiye.com"; // Turkish characters
        String token = jwtTokenProvider.generateToken(username);

        // When
        String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);

        // Then
        assertThat(extractedUsername).isEqualTo(username);
    }

    @Test
    @DisplayName("Should extract expiration date from token correctly")
    void shouldExtractExpirationDateFromTokenCorrectly() {
        // Given
        String username = "test@example.com";
        String token = jwtTokenProvider.generateToken(username);

        // When
        Date expirationDate = jwtTokenProvider.getExpirationDateFromToken(token);

        // Then
        assertThat(expirationDate).isNotNull();
        assertThat(expirationDate.toInstant()).isAfter(Instant.now().plus(Duration.ofMinutes(14)));
        assertThat(expirationDate.toInstant()).isBefore(Instant.now().plus(Duration.ofMinutes(16)));
    }

    @Test
    @DisplayName("Should detect expired tokens")
    void shouldDetectExpiredTokens() {
        // Given - Mock short expiration time
        when(jwtProperties.getAccessTokenExpiration()).thenReturn(Duration.ofMillis(1));
        JwtTokenProvider shortExpiryProvider = new JwtTokenProvider(jwtProperties);

        String username = "test@example.com";
        String token = shortExpiryProvider.generateToken(username);

        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When & Then
        assertThat(shortExpiryProvider.isTokenExpired(token)).isTrue();
        assertThat(shortExpiryProvider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("Should handle malformed tokens gracefully")
    void shouldHandleMalformedTokensGracefully() {
        // Given
        String[] malformedTokens = {
            "malformed.token",
            "not-a-jwt-token",
            "eyJhbGciOiJSUzI1NiJ9.malformed",
            "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.invalid-signature"
        };

        // When & Then
        for (String malformedToken : malformedTokens) {
            assertThat(jwtTokenProvider.validateToken(malformedToken)).isFalse();

            assertThatThrownBy(() -> jwtTokenProvider.getUsernameFromToken(malformedToken))
                .isInstanceOfAny(MalformedJwtException.class, IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("Should include correct claims in token")
    void shouldIncludeCorrectClaimsInToken() {
        // Given
        String username = "test@example.com";

        // When
        String token = jwtTokenProvider.generateToken(username);
        Claims claims = jwtTokenProvider.getAllClaimsFromToken(token);

        // Then
        assertThat(claims.getSubject()).isEqualTo(username);
        assertThat(claims.getIssuer()).isEqualTo("bist-trading-platform-test");
        assertThat(claims.getAudience()).contains("bist-trading-users-test");
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
    }

    @Test
    @DisplayName("Should generate different tokens for same username")
    void shouldGenerateDifferentTokensForSameUsername() {
        // Given
        String username = "test@example.com";

        // When
        String token1 = jwtTokenProvider.generateToken(username);
        String token2 = jwtTokenProvider.generateToken(username);

        // Then
        assertThat(token1).isNotEqualTo(token2); // Different due to iat claim
        assertThat(jwtTokenProvider.getUsernameFromToken(token1)).isEqualTo(username);
        assertThat(jwtTokenProvider.getUsernameFromToken(token2)).isEqualTo(username);
    }

    @Test
    @DisplayName("Should handle concurrent token generation")
    void shouldHandleConcurrentTokenGeneration() throws Exception {
        // Given
        String username = "test@example.com";
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // When
        CompletableFuture<String>[] futures = new CompletableFuture[threadCount];
        for (int i = 0; i < threadCount; i++) {
            futures[i] = CompletableFuture.supplyAsync(
                () -> jwtTokenProvider.generateToken(username),
                executor
            );
        }

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures);
        allFutures.get(5, TimeUnit.SECONDS);

        // Then
        for (CompletableFuture<String> future : futures) {
            String token = future.get();
            assertThat(token).isNotNull().isNotEmpty();
            assertThat(jwtTokenProvider.validateToken(token)).isTrue();
            assertThat(jwtTokenProvider.getUsernameFromToken(token)).isEqualTo(username);
        }

        executor.shutdown();
    }

    @Test
    @DisplayName("Should handle null and empty username gracefully")
    void shouldHandleNullAndEmptyUsernameGracefully() {
        // When & Then
        assertThatThrownBy(() -> jwtTokenProvider.generateToken(null))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> jwtTokenProvider.generateToken(""))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> jwtTokenProvider.generateToken("   "))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should properly encode and decode Turkish characters")
    void shouldProperlyEncodeAndDecodeTurkishCharacters() {
        // Given
        String[] turkishUsernames = TestDataBuilder.TurkishCharacters.TURKISH_USERNAMES;

        // When & Then
        for (String username : turkishUsernames) {
            String token = jwtTokenProvider.generateToken(username + "@example.com");
            String decodedUsername = jwtTokenProvider.getUsernameFromToken(token);

            assertThat(decodedUsername)
                .isEqualTo(username + "@example.com")
                .as("Turkish characters should be preserved: %s", username);
        }
    }

    @Test
    @DisplayName("Should handle very long usernames")
    void shouldHandleVeryLongUsernames() {
        // Given
        String longUsername = "very.long.username.with.lots.of.characters@example.com".repeat(5);

        // When
        String token = jwtTokenProvider.generateToken(longUsername);

        // Then
        assertThat(token).isNotNull().isNotEmpty();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUsernameFromToken(token)).isEqualTo(longUsername);
    }

    @Test
    @DisplayName("Should get correct token expiration duration")
    void shouldGetCorrectTokenExpirationDuration() {
        // When
        long accessTokenExpiration = jwtTokenProvider.getAccessTokenExpiration();
        long refreshTokenExpiration = jwtTokenProvider.getRefreshTokenExpiration();

        // Then
        assertThat(accessTokenExpiration).isEqualTo(Duration.ofMinutes(15).toSeconds());
        assertThat(refreshTokenExpiration).isEqualTo(Duration.ofDays(7).toSeconds());
    }

    private String generateTestPrivateKey() {
        // For testing purposes, generate a simple test key
        // In real implementation, this would load from test resources
        return "test-private-key";
    }

    private String generateTestPublicKey() {
        // For testing purposes, generate a simple test key
        // In real implementation, this would load from test resources
        return "test-public-key";
    }
}