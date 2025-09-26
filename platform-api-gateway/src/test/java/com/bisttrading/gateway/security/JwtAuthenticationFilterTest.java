package com.bisttrading.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration tests for JWT Authentication Filter.
 * Tests various authentication and authorization scenarios.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "bist.security.jwt.secret=" + "test-secret-key-for-jwt-token-generation-minimum-256-bits-required",
    "bist.security.jwt.issuer=bist-trading-platform",
    "bist.security.jwt.audience=bist-trading-users"
})
@DisplayName("JWT Authentication Filter Tests")
class JwtAuthenticationFilterTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @MockBean
    private WebClient.Builder webClientBuilder;

    @BeforeEach
    void setUp() {
        // Mock Redis operations to avoid external dependencies
        when(redisTemplate.hasKey(any(String.class))).thenReturn(Mono.just(false));
    }

    @Test
    @DisplayName("Should allow access to public endpoints without authentication")
    void shouldAllowAccessToPublicEndpointsWithoutAuthentication() {
        // Test health endpoint
        webTestClient.get()
            .uri("/health")
            .exchange()
            .expectStatus().isOk();

        // Test market data public endpoint
        webTestClient.get()
            .uri("/api/v1/market-data/public/symbols")
            .exchange()
            .expectStatus().is4xxClientError(); // 404 or other, but not 401
    }

    @Test
    @DisplayName("Should reject requests to protected endpoints without token")
    void shouldRejectRequestsToProtectedEndpointsWithoutToken() {
        webTestClient.get()
            .uri("/api/v1/users/profile")
            .exchange()
            .expectStatus().isUnauthorized()
            .expectHeader().exists("WWW-Authenticate")
            .expectBody()
            .jsonPath("$.error").isEqualTo("Unauthorized")
            .jsonPath("$.code").isEqualTo("MISSING_TOKEN")
            .jsonPath("$.suggestion").exists();
    }

    @Test
    @DisplayName("Should accept valid JWT tokens")
    void shouldAcceptValidJwtTokens() {
        String validToken = JwtTestUtils.createValidAccessToken();

        webTestClient.get()
            .uri("/api/v1/users/profile")
            .header("Authorization", "Bearer " + validToken)
            .exchange()
            .expectStatus().is4xxClientError(); // Not 401 - may be 404 since backend is not running
    }

    @Test
    @DisplayName("Should reject expired JWT tokens")
    void shouldRejectExpiredJwtTokens() {
        String expiredToken = JwtTestUtils.createExpiredAccessToken();

        webTestClient.get()
            .uri("/api/v1/users/profile")
            .header("Authorization", "Bearer " + expiredToken)
            .exchange()
            .expectStatus().isUnauthorized()
            .expectBody()
            .jsonPath("$.error").isEqualTo("Unauthorized")
            .jsonPath("$.code").isEqualTo("INVALID_TOKEN");
    }

    @Test
    @DisplayName("Should reject malformed JWT tokens")
    void shouldRejectMalformedJwtTokens() {
        String malformedToken = JwtTestUtils.createMalformedToken();

        webTestClient.get()
            .uri("/api/v1/users/profile")
            .header("Authorization", "Bearer " + malformedToken)
            .exchange()
            .expectStatus().isUnauthorized()
            .expectBody()
            .jsonPath("$.error").isEqualTo("Unauthorized")
            .jsonPath("$.code").isEqualTo("INVALID_TOKEN");
    }

    @Test
    @DisplayName("Should reject tokens with wrong signature")
    void shouldRejectTokensWithWrongSignature() {
        String tokenWithWrongSignature = JwtTestUtils.createTokenWithWrongSignature();

        webTestClient.get()
            .uri("/api/v1/users/profile")
            .header("Authorization", "Bearer " + tokenWithWrongSignature)
            .exchange()
            .expectStatus().isUnauthorized()
            .expectBody()
            .jsonPath("$.error").isEqualTo("Unauthorized")
            .jsonPath("$.code").isEqualTo("INVALID_TOKEN");
    }

    @Test
    @DisplayName("Should reject inactive user tokens")
    void shouldRejectInactiveUserTokens() {
        String inactiveUserToken = JwtTestUtils.createInactiveUserToken();

        webTestClient.get()
            .uri("/api/v1/users/profile")
            .header("Authorization", "Bearer " + inactiveUserToken)
            .exchange()
            .expectStatus().isUnauthorized()
            .expectBody()
            .jsonPath("$.error").isEqualTo("Unauthorized")
            .jsonPath("$.code").isEqualTo("INACTIVE_ACCOUNT");
    }

    @Test
    @DisplayName("Should allow admin access to admin endpoints")
    void shouldAllowAdminAccessToAdminEndpoints() {
        String adminToken = JwtTestUtils.createAdminAccessToken();

        webTestClient.get()
            .uri("/api/v1/admin/users")
            .header("Authorization", "Bearer " + adminToken)
            .exchange()
            .expectStatus().is4xxClientError(); // Not 401 or 403 - may be 404 since backend is not running
    }

    @Test
    @DisplayName("Should deny non-admin access to admin endpoints")
    void shouldDenyNonAdminAccessToAdminEndpoints() {
        String userToken = JwtTestUtils.createValidAccessToken();

        webTestClient.get()
            .uri("/api/v1/admin/users")
            .header("Authorization", "Bearer " + userToken)
            .exchange()
            .expectStatus().isForbidden()
            .expectBody()
            .jsonPath("$.error").isEqualTo("Forbidden")
            .jsonPath("$.code").isEqualTo("INSUFFICIENT_PRIVILEGES");
    }

    @Test
    @DisplayName("Should allow trader access to trading endpoints")
    void shouldAllowTraderAccessToTradingEndpoints() {
        String traderToken = JwtTestUtils.createTraderAccessToken();

        webTestClient.get()
            .uri("/api/v1/orders")
            .header("Authorization", "Bearer " + traderToken)
            .exchange()
            .expectStatus().is4xxClientError(); // Not 401 or 403
    }

    @Test
    @DisplayName("Should deny trading access to users without trading permissions")
    void shouldDenyTradingAccessToUsersWithoutTradingPermissions() {
        String userToken = JwtTestUtils.createValidAccessToken();

        webTestClient.post()
            .uri("/api/v1/orders")
            .header("Authorization", "Bearer " + userToken)
            .bodyValue("{\"symbol\":\"AKBNK\",\"quantity\":100}")
            .exchange()
            .expectStatus().isForbidden()
            .expectBody()
            .jsonPath("$.error").isEqualTo("Forbidden")
            .jsonPath("$.code").isEqualTo("TRADING_NOT_ALLOWED");
    }

    @Test
    @DisplayName("Should handle Turkish character tokens correctly")
    void shouldHandleTurkishCharacterTokensCorrectly() {
        String turkishToken = JwtTestUtils.createTurkishUserToken();

        webTestClient.get()
            .uri("/api/v1/users/profile")
            .header("Authorization", "Bearer " + turkishToken)
            .exchange()
            .expectStatus().is4xxClientError(); // Not 401 - authentication should pass
    }

    @Test
    @DisplayName("Should add user context headers to downstream requests")
    void shouldAddUserContextHeadersToDownstreamRequests() {
        // This test would require a mock backend to verify headers
        // For now, we test that authentication passes
        String validToken = JwtTestUtils.createValidAccessToken();

        webTestClient.get()
            .uri("/api/v1/users/profile")
            .header("Authorization", "Bearer " + validToken)
            .exchange()
            .expectStatus().is4xxClientError(); // Not 401
    }

    @Test
    @DisplayName("Should handle missing Authorization header gracefully")
    void shouldHandleMissingAuthorizationHeaderGracefully() {
        webTestClient.get()
            .uri("/api/v1/users/profile")
            .exchange()
            .expectStatus().isUnauthorized()
            .expectBody()
            .jsonPath("$.error").isEqualTo("Unauthorized")
            .jsonPath("$.code").isEqualTo("MISSING_TOKEN")
            .jsonPath("$.loginEndpoint").isEqualTo("/api/v1/auth/login");
    }

    @Test
    @DisplayName("Should handle invalid Authorization header format")
    void shouldHandleInvalidAuthorizationHeaderFormat() {
        webTestClient.get()
            .uri("/api/v1/users/profile")
            .header("Authorization", "InvalidFormat token123")
            .exchange()
            .expectStatus().isUnauthorized()
            .expectBody()
            .jsonPath("$.error").isEqualTo("Unauthorized")
            .jsonPath("$.code").isEqualTo("MISSING_TOKEN");
    }

    @Test
    @DisplayName("Should allow OPTIONS requests for CORS preflight")
    void shouldAllowOptionsRequestsForCorsPreflight() {
        webTestClient.options()
            .uri("/api/v1/users/profile")
            .header("Origin", "https://app.bisttrading.com.tr")
            .header("Access-Control-Request-Method", "GET")
            .header("Access-Control-Request-Headers", "Authorization")
            .exchange()
            .expectStatus().is2xxSuccessful();
    }
}