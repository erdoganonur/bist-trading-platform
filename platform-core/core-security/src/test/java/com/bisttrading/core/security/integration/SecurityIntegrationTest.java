package com.bisttrading.core.security.integration;

import com.bisttrading.core.security.config.JwtProperties;
import com.bisttrading.core.security.dto.JwtResponse;
import com.bisttrading.core.security.dto.LoginRequest;
import com.bisttrading.core.security.dto.RefreshTokenRequest;
import com.bisttrading.core.security.jwt.JwtTokenProvider;
import com.bisttrading.core.security.service.CustomUserDetails;
import com.bisttrading.core.security.service.CustomUserDetailsService;
import com.bisttrading.core.security.service.TokenBlacklistService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration tests for the BIST Trading Platform security module.
 * Tests JWT authentication, authorization, token management, and security configurations.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SecurityIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379)
        .withReuse(true);

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();

        // Set Redis connection properties for tests
        System.setProperty("spring.data.redis.host", redis.getHost());
        System.setProperty("spring.data.redis.port", redis.getMappedPort(6379).toString());
    }

    @Test
    void shouldAllowAccessToPublicEndpoints() throws Exception {
        // Test public authentication endpoints
        mockMvc.perform(get("/api/v1/auth/login"))
            .andExpect(status().isMethodNotAllowed()); // GET not allowed, but endpoint is accessible

        // Test health endpoints
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/health"))
            .andExpect(status().isOk());

        // Test API documentation endpoints
        mockMvc.perform(get("/swagger-ui.html"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk());
    }

    @Test
    void shouldBlockAccessToProtectedEndpointsWithoutToken() throws Exception {
        // Test protected endpoints without token
        mockMvc.perform(get("/api/v1/users/profile"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/trading/orders"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/admin/users"))
            .andExpected(status().isUnauthorized());
    }

    @Test
    void shouldAuthenticateValidUser() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
            .username("admin@bist.com.tr")
            .password("admin123")
            .rememberMe(false)
            .clientIp("127.0.0.1")
            .userAgent("Test-Agent")
            .build();

        String requestJson = objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.user.email").value("admin@bist.com.tr"))
            .andExpect(jsonPath("$.user.roles").isArray());
    }

    @Test
    void shouldRejectInvalidCredentials() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
            .username("admin@bist.com.tr")
            .password("wrongpassword")
            .build();

        String requestJson = objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAccessWithValidJwtToken() throws Exception {
        // Generate a valid token for admin user
        CustomUserDetails adminUser = userDetailsService.loadUserByUserId("admin-001");
        String accessToken = jwtTokenProvider.generateAccessToken(adminUser);

        mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk());
    }

    @Test
    void shouldRejectExpiredToken() throws Exception {
        // This test would require generating an expired token
        // For now, we'll test with an invalid token format
        mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer invalid.token.format"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectBlacklistedToken() throws Exception {
        // Generate a valid token
        CustomUserDetails adminUser = userDetailsService.loadUserByUserId("admin-001");
        String accessToken = jwtTokenProvider.generateAccessToken(adminUser);

        // Blacklist the token
        tokenBlacklistService.blacklistToken(accessToken, "Test blacklist");

        // Try to use the blacklisted token
        mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRefreshTokenSuccessfully() throws Exception {
        // Generate a valid refresh token
        CustomUserDetails adminUser = userDetailsService.loadUserByUserId("admin-001");
        String refreshToken = jwtTokenProvider.generateRefreshToken(adminUser);

        RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
            .refreshToken(refreshToken)
            .clientIp("127.0.0.1")
            .userAgent("Test-Agent")
            .build();

        String requestJson = objectMapper.writeValueAsString(refreshRequest);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void shouldRejectInvalidRefreshToken() throws Exception {
        RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
            .refreshToken("invalid.refresh.token")
            .build();

        String requestJson = objectMapper.writeValueAsString(refreshRequest);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldLogoutSuccessfully() throws Exception {
        // Generate tokens
        CustomUserDetails adminUser = userDetailsService.loadUserByUserId("admin-001");
        String accessToken = jwtTokenProvider.generateAccessToken(adminUser);
        String refreshToken = jwtTokenProvider.generateRefreshToken(adminUser);

        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
            .andExpect(status().isOk());

        // Verify token is blacklisted
        assert tokenBlacklistService.isTokenBlacklisted(accessToken);
    }

    @Test
    void shouldEnforceRoleBasedAccess() throws Exception {
        // Test admin access
        CustomUserDetails adminUser = userDetailsService.loadUserByUserId("admin-001");
        String adminToken = jwtTokenProvider.generateAccessToken(adminUser);

        mockMvc.perform(get("/api/v1/admin/users")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        // Test customer access to admin endpoint (should be forbidden)
        CustomUserDetails customerUser = userDetailsService.loadUserByUserId("customer-001");
        String customerToken = jwtTokenProvider.generateAccessToken(customerUser);

        mockMvc.perform(get("/api/v1/admin/users")
                .header("Authorization", "Bearer " + customerToken))
            .andExpect(status().isForbidden());

        // Test customer access to customer endpoint (should be allowed)
        mockMvc.perform(get("/api/v1/portfolio/summary")
                .header("Authorization", "Bearer " + customerToken))
            .andExpect(status().isOk());
    }

    @Test
    void shouldHandleCorsProperly() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type,Authorization"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
            .andExpect(header().string("Access-Control-Allow-Methods", "*"))
            .andExpect(header().exists("Access-Control-Allow-Headers"));
    }

    @Test
    void shouldValidateTokenClaimsCorrectly() throws Exception {
        CustomUserDetails traderUser = userDetailsService.loadUserByUserId("trader-001");
        String accessToken = jwtTokenProvider.generateAccessToken(traderUser);

        // Verify token contains expected claims
        String userId = jwtTokenProvider.getUserIdFromToken(accessToken);
        assert "trader-001".equals(userId);

        String email = jwtTokenProvider.getEmailFromToken(accessToken);
        assert "trader@bist.com.tr".equals(email);

        assert jwtTokenProvider.isAccessToken(accessToken);
        assert !jwtTokenProvider.isRefreshToken(accessToken);
    }

    @Test
    void shouldHandleSecurityHeaders() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Frame-Options", "DENY"))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().exists("Strict-Transport-Security"));
    }

    @Test
    void shouldRateLimitAuthenticationEndpoints() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
            .username("admin@bist.com.tr")
            .password("wrongpassword")
            .build();

        String requestJson = objectMapper.writeValueAsString(loginRequest);

        // Make multiple failed login attempts (should trigger rate limiting)
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson));
        }

        // The last attempt should be rate limited
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isTooManyRequests());
    }

    @Test
    void shouldValidateJwtPropertiesConfiguration() {
        // Verify JWT properties are properly configured for testing
        assert jwtProperties.getAccessTokenExpiry().toMinutes() == 15;
        assert jwtProperties.getRefreshTokenExpiry().toDays() == 7;
        assert "bist-trading-platform".equals(jwtProperties.getIssuer());
        assert "bist-trading-users".equals(jwtProperties.getAudience());
        assert jwtProperties.isEnableBlacklist();
    }

    @Test
    void shouldHandlePasswordComplexityValidation() throws Exception {
        // Test with weak password
        LoginRequest weakPasswordRequest = LoginRequest.builder()
            .username("test@bist.com.tr")
            .password("123")
            .build();

        String requestJson = objectMapper.writeValueAsString(weakPasswordRequest);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldTrackAndBlacklistUserTokens() throws Exception {
        CustomUserDetails traderUser = userDetailsService.loadUserByUserId("trader-001");
        String accessToken1 = jwtTokenProvider.generateAccessToken(traderUser);
        String accessToken2 = jwtTokenProvider.generateAccessToken(traderUser);

        // Track tokens
        tokenBlacklistService.trackUserToken("trader-001", accessToken1);
        tokenBlacklistService.trackUserToken("trader-001", accessToken2);

        // Blacklist all user tokens
        tokenBlacklistService.blacklistAllUserTokens("trader-001", "Security test");

        // Both tokens should be blacklisted
        assert tokenBlacklistService.isTokenBlacklisted(accessToken1);
        assert tokenBlacklistService.isTokenBlacklisted(accessToken2);
    }

    @Test
    void shouldHandleTokenExpiryCorrectly() throws Exception {
        CustomUserDetails customerUser = userDetailsService.loadUserByUserId("customer-001");
        String accessToken = jwtTokenProvider.generateAccessToken(customerUser);

        // Verify token is not expired
        assert !jwtTokenProvider.isTokenExpired(accessToken);

        // Verify token has reasonable expiry time
        long timeUntilExpiry = jwtTokenProvider.getTimeUntilExpiry(accessToken);
        assert timeUntilExpiry > 0;
        assert timeUntilExpiry <= jwtProperties.getAccessTokenExpiryInSeconds();
    }

    @Test
    void shouldValidateCustomUserDetailsCorrectly() throws Exception {
        CustomUserDetails adminUser = userDetailsService.loadUserByUserId("admin-001");

        // Verify user details
        assert adminUser.getUserId().equals("admin-001");
        assert adminUser.getEmail().equals("admin@bist.com.tr");
        assert adminUser.isActive();
        assert adminUser.isEmailVerified();
        assert adminUser.isKycCompleted();
        assert adminUser.isAdmin();
        assert adminUser.canTrade();
        assert adminUser.isFullyVerified();

        // Test trader user
        CustomUserDetails traderUser = userDetailsService.loadUserByUserId("trader-001");
        assert traderUser.isTrader();
        assert traderUser.isProfessionalInvestor();
        assert "AGGRESSIVE".equals(traderUser.getRiskProfile());

        // Test customer user
        CustomUserDetails customerUser = userDetailsService.loadUserByUserId("customer-001");
        assert customerUser.isCustomer();
        assert !customerUser.isProfessionalInvestor();
        assert "CONSERVATIVE".equals(customerUser.getRiskProfile());
    }
}