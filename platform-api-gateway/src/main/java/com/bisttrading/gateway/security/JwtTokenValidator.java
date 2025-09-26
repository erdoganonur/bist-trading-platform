package com.bisttrading.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * JWT Token Validator for BIST Trading Platform Gateway.
 *
 * Provides comprehensive JWT token validation including:
 * - Token signature verification
 * - Expiration checking
 * - Blacklist validation
 * - Claims extraction and validation
 * - Multi-tenancy support
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenValidator {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${bist.security.jwt.secret:default-secret-key-for-jwt-validation-minimum-256-bits}")
    private String jwtSecret;

    @Value("${bist.security.jwt.issuer:bist-trading-platform}")
    private String expectedIssuer;

    @Value("${bist.security.jwt.audience:bist-trading-users}")
    private String expectedAudience;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Validate JWT token comprehensively.
     *
     * @param token JWT token string
     * @return Mono with validation result containing claims if valid
     */
    public Mono<JwtValidationResult> validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return Mono.just(JwtValidationResult.invalid("Token is null or empty"));
        }

        try {
            // Parse and validate token
            Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .requireIssuer(expectedIssuer)
                .requireAudience(expectedAudience)
                .build()
                .parseSignedClaims(token)
                .getPayload();

            // Extract token ID for blacklist checking
            String jwtId = claims.getId();
            if (jwtId == null) {
                return Mono.just(JwtValidationResult.invalid("Token missing JTI claim"));
            }

            // Check if token is blacklisted
            return isTokenBlacklisted(jwtId)
                .map(isBlacklisted -> {
                    if (isBlacklisted) {
                        log.warn("Blacklisted token attempted access: {}", jwtId);
                        return JwtValidationResult.invalid("Token is blacklisted");
                    }

                    // Validate token type
                    String tokenType = claims.get("type", String.class);
                    if (!"access".equals(tokenType)) {
                        return JwtValidationResult.invalid("Invalid token type: " + tokenType);
                    }

                    // Extract and validate user claims
                    JwtClaims jwtClaims = extractClaims(claims);
                    return JwtValidationResult.valid(jwtClaims);
                });

        } catch (ExpiredJwtException e) {
            log.debug("Expired JWT token: {}", e.getMessage());
            return Mono.just(JwtValidationResult.invalid("Token expired"));

        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            return Mono.just(JwtValidationResult.invalid("Unsupported token"));

        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            return Mono.just(JwtValidationResult.invalid("Malformed token"));

        } catch (SecurityException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return Mono.just(JwtValidationResult.invalid("Invalid token"));

        } catch (Exception e) {
            log.error("Unexpected error validating JWT token", e);
            return Mono.just(JwtValidationResult.invalid("Token validation failed"));
        }
    }

    /**
     * Extract JWT token from Authorization header or cookies.
     */
    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * Check if token is in Redis blacklist.
     */
    private Mono<Boolean> isTokenBlacklisted(String jwtId) {
        String blacklistKey = "jwt:blacklist:" + jwtId;
        return redisTemplate.hasKey(blacklistKey)
            .onErrorReturn(false); // If Redis is down, allow token (fail open)
    }

    /**
     * Extract and validate claims from JWT token.
     */
    private JwtClaims extractClaims(Claims claims) {
        JwtClaims.JwtClaimsBuilder builder = JwtClaims.builder()
            .jwtId(claims.getId())
            .subject(claims.getSubject())
            .issuer(claims.getIssuer())
            .audience(claims.getAudience())
            .issuedAt(claims.getIssuedAt().toInstant())
            .expiresAt(claims.getExpiration().toInstant())
            .userId(claims.get("userId", String.class))
            .email(claims.get("email", String.class))
            .username(claims.get("username", String.class));

        // Extract roles
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        if (roles != null) {
            builder.roles(roles);
        }

        // Extract organization info for multi-tenancy
        String organizationId = claims.get("organizationId", String.class);
        if (organizationId != null) {
            builder.organizationId(organizationId);
        }

        // Extract user profile information
        builder.firstName(claims.get("firstName", String.class))
            .lastName(claims.get("lastName", String.class))
            .active(claims.get("active", Boolean.class));

        // Extract custom claims
        @SuppressWarnings("unchecked")
        Map<String, Object> customClaims = claims.get("customClaims", Map.class);
        if (customClaims != null) {
            builder.customClaims(customClaims);
        }

        return builder.build();
    }

    /**
     * Validate token for specific role requirements.
     */
    public Mono<Boolean> hasRequiredRole(String token, String requiredRole) {
        return validateToken(token)
            .map(result -> {
                if (!result.isValid()) {
                    return false;
                }

                List<String> userRoles = result.getClaims().getRoles();
                return userRoles != null && userRoles.contains(requiredRole);
            });
    }

    /**
     * Validate token for organization access.
     */
    public Mono<Boolean> hasOrganizationAccess(String token, String organizationId) {
        return validateToken(token)
            .map(result -> {
                if (!result.isValid()) {
                    return false;
                }

                String tokenOrgId = result.getClaims().getOrganizationId();
                return organizationId.equals(tokenOrgId);
            });
    }

    /**
     * Get time until token expiry.
     */
    public Mono<Duration> getTimeUntilExpiry(String token) {
        return validateToken(token)
            .map(result -> {
                if (!result.isValid()) {
                    return Duration.ZERO;
                }

                Instant expiresAt = result.getClaims().getExpiresAt();
                Instant now = Instant.now();

                return expiresAt.isAfter(now) ? Duration.between(now, expiresAt) : Duration.ZERO;
            });
    }

    /**
     * Blacklist a token (add to Redis blacklist).
     */
    public Mono<Void> blacklistToken(String token, String reason) {
        return validateToken(token)
            .filter(JwtValidationResult::isValid)
            .flatMap(result -> {
                JwtClaims claims = result.getClaims();
                String blacklistKey = "jwt:blacklist:" + claims.getJwtId();

                // Calculate TTL based on token expiry
                Duration ttl = Duration.between(Instant.now(), claims.getExpiresAt());
                if (ttl.isNegative()) {
                    ttl = Duration.ofMinutes(5); // Minimum TTL for expired tokens
                }

                String blacklistData = String.format(
                    "{\"reason\":\"%s\",\"blacklistedAt\":\"%s\",\"userId\":\"%s\"}",
                    reason, Instant.now(), claims.getUserId()
                );

                return redisTemplate.opsForValue()
                    .set(blacklistKey, blacklistData, ttl)
                    .then();
            })
            .doOnSuccess(v -> log.info("Token blacklisted successfully"))
            .doOnError(e -> log.error("Failed to blacklist token", e))
            .then();
    }

    /**
     * JWT validation result container.
     */
    public static class JwtValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final JwtClaims claims;

        private JwtValidationResult(boolean valid, String errorMessage, JwtClaims claims) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.claims = claims;
        }

        public static JwtValidationResult valid(JwtClaims claims) {
            return new JwtValidationResult(true, null, claims);
        }

        public static JwtValidationResult invalid(String errorMessage) {
            return new JwtValidationResult(false, errorMessage, null);
        }

        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
        public JwtClaims getClaims() { return claims; }
    }
}