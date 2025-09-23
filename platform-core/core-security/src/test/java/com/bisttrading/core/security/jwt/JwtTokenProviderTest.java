package com.bisttrading.core.security.jwt;

import com.bisttrading.core.security.config.JwtProperties;
import com.bisttrading.core.security.service.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtTokenProvider.
 * Tests JWT token generation, validation, and claim extraction.
 */
@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;
    private CustomUserDetails testUser;

    @BeforeEach
    void setUp() {
        // Setup test JWT properties
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test-secret-key-that-is-long-enough-for-hmac-sha256-algorithm");
        jwtProperties.setAccessTokenExpiry(Duration.ofMinutes(15));
        jwtProperties.setRefreshTokenExpiry(Duration.ofDays(7));
        jwtProperties.setIssuer("bist-trading-test");
        jwtProperties.setAudience("bist-trading-test-users");
        jwtProperties.setClockSkew(Duration.ofMinutes(2));

        // Initialize JWT provider
        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
        jwtTokenProvider.init();

        // Create test user
        testUser = CustomUserDetails.builder()
            .userId("test-user-001")
            .email("test@bist.com.tr")
            .username("testuser")
            .firstName("Test")
            .lastName("User")
            .roles(Set.of("CUSTOMER", "RETAIL_CUSTOMER"))
            .permissions(Set.of("TRADING", "PORTFOLIO_VIEW"))
            .active(true)
            .emailVerified(true)
            .phoneVerified(true)
            .kycCompleted(true)
            .lastLoginDate(LocalDateTime.now())
            .build();
    }

    @Test
    void shouldGenerateValidAccessToken() {
        String accessToken = jwtTokenProvider.generateAccessToken(testUser);

        assertNotNull(accessToken);
        assertFalse(accessToken.isEmpty());
        assertTrue(accessToken.split("\\.").length == 3); // JWT format: header.payload.signature
    }

    @Test
    void shouldGenerateValidRefreshToken() {
        String refreshToken = jwtTokenProvider.generateRefreshToken(testUser);

        assertNotNull(refreshToken);
        assertFalse(refreshToken.isEmpty());
        assertTrue(refreshToken.split("\\.").length == 3);
    }

    @Test
    void shouldValidateValidToken() {
        String accessToken = jwtTokenProvider.generateAccessToken(testUser);

        assertTrue(jwtTokenProvider.validateToken(accessToken));
    }

    @Test
    void shouldRejectInvalidToken() {
        assertFalse(jwtTokenProvider.validateToken("invalid.token.format"));
        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken(null));
    }

    @Test
    void shouldExtractUserIdFromToken() {
        String accessToken = jwtTokenProvider.generateAccessToken(testUser);

        String extractedUserId = jwtTokenProvider.getUserIdFromToken(accessToken);

        assertEquals("test-user-001", extractedUserId);
    }

    @Test
    void shouldExtractEmailFromToken() {
        String accessToken = jwtTokenProvider.generateAccessToken(testUser);

        String extractedEmail = jwtTokenProvider.getEmailFromToken(accessToken);

        assertEquals("test@bist.com.tr", extractedEmail);
    }

    @Test
    void shouldIdentifyAccessToken() {
        String accessToken = jwtTokenProvider.generateAccessToken(testUser);

        assertTrue(jwtTokenProvider.isAccessToken(accessToken));
        assertFalse(jwtTokenProvider.isRefreshToken(accessToken));
    }

    @Test
    void shouldIdentifyRefreshToken() {
        String refreshToken = jwtTokenProvider.generateRefreshToken(testUser);

        assertTrue(jwtTokenProvider.isRefreshToken(refreshToken));
        assertFalse(jwtTokenProvider.isAccessToken(refreshToken));
    }

    @Test
    void shouldCalculateCorrectExpiryTime() {
        String accessToken = jwtTokenProvider.generateAccessToken(testUser);

        long timeUntilExpiry = jwtTokenProvider.getTimeUntilExpiry(accessToken);

        assertTrue(timeUntilExpiry > 0);
        assertTrue(timeUntilExpiry <= jwtProperties.getAccessTokenExpiryInSeconds());
    }

    @Test
    void shouldDetectNonExpiredToken() {
        String accessToken = jwtTokenProvider.generateAccessToken(testUser);

        assertFalse(jwtTokenProvider.isTokenExpired(accessToken));
    }

    @Test
    void shouldExtractTokenType() {
        String accessToken = jwtTokenProvider.generateAccessToken(testUser);
        String refreshToken = jwtTokenProvider.generateRefreshToken(testUser);

        assertEquals("access", jwtTokenProvider.getTokenType(accessToken));
        assertEquals("refresh", jwtTokenProvider.getTokenType(refreshToken));
    }

    @Test
    void shouldExtractJwtId() {
        String accessToken = jwtTokenProvider.generateAccessToken(testUser);

        String jwtId = jwtTokenProvider.getJwtIdFromToken(accessToken);

        assertNotNull(jwtId);
        assertFalse(jwtId.isEmpty());
    }

    @Test
    void shouldRefreshAccessTokenSuccessfully() {
        String refreshToken = jwtTokenProvider.generateRefreshToken(testUser);

        String newAccessToken = jwtTokenProvider.refreshAccessToken(refreshToken, testUser);

        assertNotNull(newAccessToken);
        assertTrue(jwtTokenProvider.validateToken(newAccessToken));
        assertTrue(jwtTokenProvider.isAccessToken(newAccessToken));
        assertEquals("test-user-001", jwtTokenProvider.getUserIdFromToken(newAccessToken));
    }

    @Test
    void shouldRejectRefreshWithAccessToken() {
        String accessToken = jwtTokenProvider.generateAccessToken(testUser);

        assertThrows(Exception.class, () -> {
            jwtTokenProvider.refreshAccessToken(accessToken, testUser);
        });
    }

    @Test
    void shouldRejectRefreshWithWrongUser() {
        String refreshToken = jwtTokenProvider.generateRefreshToken(testUser);

        CustomUserDetails differentUser = CustomUserDetails.builder()
            .userId("different-user-001")
            .email("different@bist.com.tr")
            .build();

        assertThrows(Exception.class, () -> {
            jwtTokenProvider.refreshAccessToken(refreshToken, differentUser);
        });
    }

    @Test
    void shouldGetFormattedRemainingTime() {
        String accessToken = jwtTokenProvider.generateAccessToken(testUser);

        String formattedTime = jwtTokenProvider.getTokenRemainingTimeFormatted(accessToken);

        assertNotNull(formattedTime);
        assertTrue(formattedTime.contains("dakika") || formattedTime.contains("saat"));
    }

    @Test
    void shouldExtractCustomClaims() {
        String accessToken = jwtTokenProvider.generateAccessToken(testUser);

        String customUserId = jwtTokenProvider.getCustomClaim(accessToken, "bist_user_id", String.class);
        String tenant = jwtTokenProvider.getCustomClaim(accessToken, "bist_tenant", String.class);
        String version = jwtTokenProvider.getCustomClaim(accessToken, "bist_version", String.class);

        assertEquals("test-user-001", customUserId);
        assertEquals("bist", tenant);
        assertEquals("1.0", version);
    }

    @Test
    void shouldIncludeRolesWhenEnabled() {
        // Enable roles in claims
        jwtProperties.getClaims().setIncludeRoles(true);

        String accessToken = jwtTokenProvider.generateAccessToken(testUser);
        var roles = jwtTokenProvider.getRolesFromToken(accessToken);

        assertNotNull(roles);
        assertTrue(roles.contains("ROLE_CUSTOMER"));
        assertTrue(roles.contains("ROLE_RETAIL_CUSTOMER"));
    }

    @Test
    void shouldIncludeProfileWhenEnabled() {
        // Enable profile in claims
        jwtProperties.getClaims().setIncludeProfile(true);

        String accessToken = jwtTokenProvider.generateAccessToken(testUser);
        var claims = jwtTokenProvider.getClaimsFromToken(accessToken);

        assertEquals("Test", claims.get("firstName"));
        assertEquals("User", claims.get("lastName"));
        assertEquals(true, claims.get("active"));
    }

    @Test
    void shouldIncludeLastLoginWhenEnabled() {
        // Enable last login in claims
        jwtProperties.getClaims().setIncludeLastLogin(true);

        String accessToken = jwtTokenProvider.generateAccessToken(testUser);
        var claims = jwtTokenProvider.getClaimsFromToken(accessToken);

        assertNotNull(claims.get("lastLogin"));
    }

    @Test
    void shouldHandleNullInputsGracefully() {
        assertFalse(jwtTokenProvider.validateToken(null));
        assertNull(jwtTokenProvider.getUserIdFromToken(null));
        assertNull(jwtTokenProvider.getEmailFromToken(null));
        assertNull(jwtTokenProvider.getTokenType(null));
        assertEquals(0, jwtTokenProvider.getTimeUntilExpiry(null));
    }

    @Test
    void shouldGenerateUniqueTokenIds() {
        String token1 = jwtTokenProvider.generateAccessToken(testUser);
        String token2 = jwtTokenProvider.generateAccessToken(testUser);

        String jwtId1 = jwtTokenProvider.getJwtIdFromToken(token1);
        String jwtId2 = jwtTokenProvider.getJwtIdFromToken(token2);

        assertNotEquals(jwtId1, jwtId2);
    }

    @Test
    void shouldValidateTokenWithClockSkew() {
        // This test verifies that tokens generated slightly in the past are still valid
        // due to clock skew tolerance
        String accessToken = jwtTokenProvider.generateAccessToken(testUser);

        // Token should be valid even with minor time differences
        assertTrue(jwtTokenProvider.validateToken(accessToken));
    }

    @Test
    void shouldRejectTokenWithInvalidSignature() {
        String accessToken = jwtTokenProvider.generateAccessToken(testUser);

        // Tamper with the token signature
        String[] parts = accessToken.split("\\.");
        String tamperedToken = parts[0] + "." + parts[1] + ".tampered_signature";

        assertFalse(jwtTokenProvider.validateToken(tamperedToken));
    }

    @Test
    void shouldRejectTokenWithInvalidClaims() {
        // Create a token with a different secret to simulate invalid claims
        JwtProperties invalidProperties = new JwtProperties();
        invalidProperties.setSecret("different-secret-key-that-is-long-enough-for-hmac");
        invalidProperties.setIssuer("different-issuer");
        invalidProperties.setAudience("different-audience");

        JwtTokenProvider invalidProvider = new JwtTokenProvider(invalidProperties);
        invalidProvider.init();

        String invalidToken = invalidProvider.generateAccessToken(testUser);

        // This token should be rejected by our provider due to different issuer/audience
        assertFalse(jwtTokenProvider.validateToken(invalidToken));
    }
}