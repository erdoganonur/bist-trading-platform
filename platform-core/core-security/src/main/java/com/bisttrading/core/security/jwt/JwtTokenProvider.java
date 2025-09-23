package com.bisttrading.core.security.jwt;

import com.bisttrading.core.common.exceptions.TechnicalException;
import com.bisttrading.core.common.constants.ErrorCodes;
import com.bisttrading.core.security.config.JwtProperties;
import com.bisttrading.core.security.service.CustomUserDetails;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JWT Token Provider for the BIST Trading Platform.
 * Handles JWT token generation, validation, and claim extraction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;
    private JwtParser jwtParser;

    @PostConstruct
    public void init() {
        // Validate JWT properties
        jwtProperties.validate();

        // Initialize secret key
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());

        // Initialize JWT parser with validation
        this.jwtParser = Jwts.parser()
            .verifyWith(secretKey)
            .requireIssuer(jwtProperties.getIssuer())
            .requireAudience(jwtProperties.getAudience())
            .clockSkewSeconds(jwtProperties.getClockSkewInSeconds())
            .build();

        log.info("JWT Token Provider başlatıldı - issuer: {}, audience: {}",
            jwtProperties.getIssuer(), jwtProperties.getAudience());
    }

    /**
     * Generates an access token for the authenticated user.
     *
     * @param authentication Spring Security authentication object
     * @return JWT access token
     */
    public String generateAccessToken(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return generateAccessToken(userDetails);
    }

    /**
     * Generates an access token for the given user details.
     *
     * @param userDetails Custom user details
     * @return JWT access token
     */
    public String generateAccessToken(CustomUserDetails userDetails) {
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtProperties.getAccessTokenExpiry());

        JwtBuilder builder = Jwts.builder()
            .subject(userDetails.getUserId())
            .issuer(jwtProperties.getIssuer())
            .audience().add(jwtProperties.getAudience()).and()
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .id(UUID.randomUUID().toString())
            .claim("type", "access")
            .claim("email", userDetails.getEmail())
            .claim("username", userDetails.getUsername());

        // Add roles if enabled
        if (jwtProperties.getClaims().isIncludeRoles()) {
            List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
            builder.claim("roles", roles);
        }

        // Add profile information if enabled
        if (jwtProperties.getClaims().isIncludeProfile()) {
            builder.claim("firstName", userDetails.getFirstName())
                   .claim("lastName", userDetails.getLastName())
                   .claim("active", userDetails.isActive());
        }

        // Add last login if enabled
        if (jwtProperties.getClaims().isIncludeLastLogin() && userDetails.getLastLoginDate() != null) {
            builder.claim("lastLogin", userDetails.getLastLoginDate().toString());
        }

        // Add custom claims with prefix
        String prefix = jwtProperties.getClaims().getCustomClaimPrefix();
        builder.claim(prefix + "user_id", userDetails.getUserId())
               .claim(prefix + "tenant", "bist")
               .claim(prefix + "version", "1.0");

        try {
            String token = builder.signWith(secretKey).compact();
            log.debug("Access token oluşturuldu - userId: {}, expiry: {}",
                userDetails.getUserId(), expiry);
            return token;
        } catch (Exception e) {
            log.error("Access token oluşturulurken hata: {}", e.getMessage(), e);
            throw new TechnicalException(ErrorCodes.INTERNAL_SERVER_ERROR,
                "Token oluşturulurken hata oluştu", e);
        }
    }

    /**
     * Generates a refresh token for the given user.
     *
     * @param userDetails Custom user details
     * @return JWT refresh token
     */
    public String generateRefreshToken(CustomUserDetails userDetails) {
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtProperties.getRefreshTokenExpiry());

        try {
            String token = Jwts.builder()
                .subject(userDetails.getUserId())
                .issuer(jwtProperties.getIssuer())
                .audience().add(jwtProperties.getAudience()).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .id(UUID.randomUUID().toString())
                .claim("type", "refresh")
                .claim("email", userDetails.getEmail())
                .signWith(secretKey)
                .compact();

            log.debug("Refresh token oluşturuldu - userId: {}, expiry: {}",
                userDetails.getUserId(), expiry);
            return token;
        } catch (Exception e) {
            log.error("Refresh token oluşturulurken hata: {}", e.getMessage(), e);
            throw new TechnicalException(ErrorCodes.INTERNAL_SERVER_ERROR,
                "Refresh token oluşturulurken hata oluştu", e);
        }
    }

    /**
     * Validates a JWT token.
     *
     * @param token JWT token to validate
     * @return true if token is valid
     */
    public boolean validateToken(String token) {
        try {
            jwtParser.parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("Token süresi dolmuş: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("Desteklenmeyen JWT token: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn("Geçersiz JWT token formatı: {}", e.getMessage());
            return false;
        } catch (SecurityException e) {
            log.warn("JWT güvenlik hatası: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("JWT token boş veya null: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("JWT token doğrulama hatası: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Extracts user ID from JWT token.
     *
     * @param token JWT token
     * @return User ID
     */
    public String getUserIdFromToken(String token) {
        try {
            Claims claims = jwtParser.parseSignedClaims(token).getPayload();
            return claims.getSubject();
        } catch (Exception e) {
            log.error("Token'dan kullanıcı ID'si alınırken hata: {}", e.getMessage());
            throw new TechnicalException(ErrorCodes.TOKEN_INVALID,
                "Token'dan kullanıcı bilgisi alınamadı", e);
        }
    }

    /**
     * Extracts email from JWT token.
     *
     * @param token JWT token
     * @return User email
     */
    public String getEmailFromToken(String token) {
        try {
            Claims claims = jwtParser.parseSignedClaims(token).getPayload();
            return claims.get("email", String.class);
        } catch (Exception e) {
            log.error("Token'dan email alınırken hata: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extracts roles from JWT token.
     *
     * @param token JWT token
     * @return List of user roles
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        try {
            Claims claims = jwtParser.parseSignedClaims(token).getPayload();
            return claims.get("roles", List.class);
        } catch (Exception e) {
            log.error("Token'dan roller alınırken hata: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Extracts all claims from JWT token.
     *
     * @param token JWT token
     * @return JWT claims
     */
    public Claims getClaimsFromToken(String token) {
        try {
            return jwtParser.parseSignedClaims(token).getPayload();
        } catch (Exception e) {
            log.error("Token'dan claims alınırken hata: {}", e.getMessage());
            throw new TechnicalException(ErrorCodes.TOKEN_INVALID,
                "Token claims okunamadı", e);
        }
    }

    /**
     * Gets token expiry date.
     *
     * @param token JWT token
     * @return Token expiry date
     */
    public LocalDateTime getExpiryFromToken(String token) {
        try {
            Claims claims = jwtParser.parseSignedClaims(token).getPayload();
            return LocalDateTime.ofInstant(claims.getExpiration().toInstant(), ZoneId.systemDefault());
        } catch (Exception e) {
            log.error("Token'dan expiry alınırken hata: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Gets token type (access/refresh).
     *
     * @param token JWT token
     * @return Token type
     */
    public String getTokenType(String token) {
        try {
            Claims claims = jwtParser.parseSignedClaims(token).getPayload();
            return claims.get("type", String.class);
        } catch (Exception e) {
            log.error("Token'dan tip alınırken hata: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Gets JWT ID from token.
     *
     * @param token JWT token
     * @return JWT ID
     */
    public String getJwtIdFromToken(String token) {
        try {
            Claims claims = jwtParser.parseSignedClaims(token).getPayload();
            return claims.getId();
        } catch (Exception e) {
            log.error("Token'dan JWT ID alınırken hata: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Checks if token is expired.
     *
     * @param token JWT token
     * @return true if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = jwtParser.parseSignedClaims(token).getPayload();
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            log.error("Token expiry kontrolü hatası: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Checks if token is of access type.
     *
     * @param token JWT token
     * @return true if access token
     */
    public boolean isAccessToken(String token) {
        String tokenType = getTokenType(token);
        return "access".equals(tokenType);
    }

    /**
     * Checks if token is of refresh type.
     *
     * @param token JWT token
     * @return true if refresh token
     */
    public boolean isRefreshToken(String token) {
        String tokenType = getTokenType(token);
        return "refresh".equals(tokenType);
    }

    /**
     * Gets time until token expiry in seconds.
     *
     * @param token JWT token
     * @return Seconds until expiry, or 0 if expired/invalid
     */
    public long getTimeUntilExpiry(String token) {
        try {
            Claims claims = jwtParser.parseSignedClaims(token).getPayload();
            long expiryTime = claims.getExpiration().toInstant().getEpochSecond();
            long currentTime = Instant.now().getEpochSecond();
            return Math.max(0, expiryTime - currentTime);
        } catch (Exception e) {
            log.error("Token expiry süresi hesaplanırken hata: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Extracts custom claim from token.
     *
     * @param token JWT token
     * @param claimName Claim name
     * @param claimType Claim type
     * @return Claim value
     */
    public <T> T getCustomClaim(String token, String claimName, Class<T> claimType) {
        try {
            Claims claims = jwtParser.parseSignedClaims(token).getPayload();
            return claims.get(claimName, claimType);
        } catch (Exception e) {
            log.error("Custom claim alınırken hata - claim: {}, error: {}", claimName, e.getMessage());
            return null;
        }
    }

    /**
     * Creates a new access token from a refresh token.
     *
     * @param refreshToken Valid refresh token
     * @param userDetails Updated user details
     * @return New access token
     */
    public String refreshAccessToken(String refreshToken, CustomUserDetails userDetails) {
        if (!validateToken(refreshToken) || !isRefreshToken(refreshToken)) {
            throw new TechnicalException(ErrorCodes.TOKEN_INVALID,
                "Geçersiz refresh token");
        }

        String userIdFromToken = getUserIdFromToken(refreshToken);
        if (!userIdFromToken.equals(userDetails.getUserId())) {
            throw new TechnicalException(ErrorCodes.TOKEN_INVALID,
                "Refresh token kullanıcı ID'si eşleşmiyor");
        }

        return generateAccessToken(userDetails);
    }

    /**
     * Gets token remaining time as a human-readable string.
     *
     * @param token JWT token
     * @return Human-readable remaining time
     */
    public String getTokenRemainingTimeFormatted(String token) {
        long secondsUntilExpiry = getTimeUntilExpiry(token);

        if (secondsUntilExpiry <= 0) {
            return "Süresi dolmuş";
        }

        long minutes = secondsUntilExpiry / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return String.format("%d gün %d saat", days, hours % 24);
        } else if (hours > 0) {
            return String.format("%d saat %d dakika", hours, minutes % 60);
        } else {
            return String.format("%d dakika", minutes);
        }
    }
}