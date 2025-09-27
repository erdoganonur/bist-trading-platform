package com.bisttrading.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JWT Test Utilities for Gateway Security Tests.
 *
 * Provides helper methods for creating test JWT tokens with various
 * configurations and claims for comprehensive testing scenarios.
 */
public final class JwtTestUtils {

    private JwtTestUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static final String TEST_SECRET = "test-secret-key-for-jwt-token-generation-minimum-256-bits-required";
    private static final String TEST_ISSUER = "bist-trading-platform";
    private static final String TEST_AUDIENCE = "bist-trading-users";

    private static final SecretKey SIGNING_KEY = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));

    /**
     * Create a valid access token for testing.
     */
    public static String createValidAccessToken() {
        return createAccessToken(
            "test-user-123",
            "testuser@example.com",
            "testuser",
            List.of("USER"),
            Map.of()
        );
    }

    /**
     * Create an admin access token for testing.
     */
    public static String createAdminAccessToken() {
        return createAccessToken(
            "admin-user-456",
            "admin@bisttrading.com.tr",
            "admin",
            List.of("ADMIN", "USER"),
            Map.of(
                "firstName", "Admin",
                "lastName", "User",
                "organizationId", "org-123"
            )
        );
    }

    /**
     * Create a trader access token with trading permissions.
     */
    public static String createTraderAccessToken() {
        return createAccessToken(
            "trader-user-789",
            "trader@example.com",
            "trader",
            List.of("TRADER", "USER"),
            Map.of(
                "firstName", "John",
                "lastName", "Trader",
                "active", true,
                "kycCompleted", true,
                "tradingStatus", "ACTIVE",
                "tradingPermissions", List.of("EQUITY", "BOND")
            )
        );
    }

    /**
     * Create an expired access token for testing.
     */
    public static String createExpiredAccessToken() {
        Instant expiredTime = Instant.now().minus(1, ChronoUnit.HOURS);

        return Jwts.builder()
            .subject("expired-user-999")
            .issuer(TEST_ISSUER)
            .audience().add(TEST_AUDIENCE).and()
            .issuedAt(Date.from(expiredTime.minus(1, ChronoUnit.HOURS)))
            .expiration(Date.from(expiredTime))
            .id(UUID.randomUUID().toString())
            .claim("type", "access")
            .claim("userId", "expired-user-999")
            .claim("email", "expired@example.com")
            .claim("username", "expired")
            .claim("roles", List.of("USER"))
            .signWith(SIGNING_KEY)
            .compact();
    }

    /**
     * Create an inactive user token for testing.
     */
    public static String createInactiveUserToken() {
        return createAccessToken(
            "inactive-user-000",
            "inactive@example.com",
            "inactive",
            List.of("USER"),
            Map.of(
                "active", false,
                "firstName", "Inactive",
                "lastName", "User"
            )
        );
    }

    /**
     * Create a refresh token for testing.
     */
    public static String createValidRefreshToken() {
        Instant now = Instant.now();

        return Jwts.builder()
            .subject("test-user-123")
            .issuer(TEST_ISSUER)
            .audience().add(TEST_AUDIENCE).and()
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(7, ChronoUnit.DAYS)))
            .id(UUID.randomUUID().toString())
            .claim("type", "refresh")
            .claim("userId", "test-user-123")
            .signWith(SIGNING_KEY)
            .compact();
    }

    /**
     * Create an access token with custom claims.
     */
    public static String createAccessToken(String userId, String email, String username,
                                         List<String> roles, Map<String, Object> customClaims) {
        Instant now = Instant.now();

        var builder = Jwts.builder()
            .subject(userId)
            .issuer(TEST_ISSUER)
            .audience().add(TEST_AUDIENCE).and()
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(15, ChronoUnit.MINUTES)))
            .id(UUID.randomUUID().toString())
            .claim("type", "access")
            .claim("userId", userId)
            .claim("email", email)
            .claim("username", username)
            .claim("roles", roles);

        // Add custom claims
        customClaims.forEach(builder::claim);

        return builder.signWith(SIGNING_KEY).compact();
    }

    /**
     * Create a malformed JWT token for testing.
     */
    public static String createMalformedToken() {
        return "this.is.not.a.valid.jwt.token";
    }

    /**
     * Create a token with wrong signature for testing.
     */
    public static String createTokenWithWrongSignature() {
        SecretKey wrongKey = Keys.hmacShaKeyFor("wrong-secret-key-for-testing-signature-validation".getBytes());
        Instant now = Instant.now();

        return Jwts.builder()
            .subject("test-user-123")
            .issuer(TEST_ISSUER)
            .audience().add(TEST_AUDIENCE).and()
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(15, ChronoUnit.MINUTES)))
            .id(UUID.randomUUID().toString())
            .claim("type", "access")
            .claim("userId", "test-user-123")
            .signWith(wrongKey)
            .compact();
    }

    /**
     * Create a token with wrong issuer for testing.
     */
    public static String createTokenWithWrongIssuer() {
        Instant now = Instant.now();

        return Jwts.builder()
            .subject("test-user-123")
            .issuer("wrong-issuer")
            .audience().add(TEST_AUDIENCE).and()
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(15, ChronoUnit.MINUTES)))
            .id(UUID.randomUUID().toString())
            .claim("type", "access")
            .claim("userId", "test-user-123")
            .signWith(SIGNING_KEY)
            .compact();
    }

    /**
     * Create a token without required claims for testing.
     */
    public static String createTokenWithoutRequiredClaims() {
        Instant now = Instant.now();

        return Jwts.builder()
            .subject("test-user-123")
            .issuer(TEST_ISSUER)
            .audience().add(TEST_AUDIENCE).and()
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(15, ChronoUnit.MINUTES)))
            // Missing JTI claim
            .claim("type", "access")
            .signWith(SIGNING_KEY)
            .compact();
    }

    /**
     * Create a token with Turkish characters for internationalization testing.
     */
    public static String createTurkishUserToken() {
        return createAccessToken(
            "turkish-user-456",
            "türkçe@example.com",
            "türkçekullanıcı",
            List.of("USER"),
            Map.of(
                "firstName", "Çağlar",
                "lastName", "Şıktırıkoğlu",
                "organizationId", "turkish-org-123"
            )
        );
    }

    /**
     * Get the test secret used for signing tokens.
     */
    public static String getTestSecret() {
        return TEST_SECRET;
    }

    /**
     * Get the test issuer.
     */
    public static String getTestIssuer() {
        return TEST_ISSUER;
    }

    /**
     * Get the test audience.
     */
    public static String getTestAudience() {
        return TEST_AUDIENCE;
    }
}