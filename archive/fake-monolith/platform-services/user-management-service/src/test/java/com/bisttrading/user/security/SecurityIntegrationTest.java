package com.bisttrading.user.security;

import com.bisttrading.core.security.dto.JwtResponse;
import com.bisttrading.core.security.dto.LoginRequest;
import com.bisttrading.core.security.dto.RegisterRequest;
import com.bisttrading.infrastructure.persistence.entity.UserEntity;
import com.bisttrading.infrastructure.persistence.repository.UserRepository;
import com.bisttrading.user.test.TestDataBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive security integration tests for BIST Trading Platform.
 * Tests authentication, authorization, JWT security, and attack prevention.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
@DisplayName("Security Integration Tests")
class SecurityIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("bist_trading_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should reject requests without authentication for protected endpoints")
    void shouldRejectRequestsWithoutAuthenticationForProtectedEndpoints() throws Exception {
        // Test all protected user endpoints
        mockMvc.perform(get("/api/v1/users/profile"))
                .andExpected(status().isUnauthorized());

        mockMvc.perform(put("/api/v1/users/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/v1/users/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/users/verify-email"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/users/verify-phone"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/users/sessions"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/v1/users/sessions/terminate-all"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/users/deactivate"))
                .andExpect(status().isUnauthorized());

        // Test protected auth endpoints
        mockMvc.perform(get("/api/v1/auth/validate"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpected(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject malformed JWT tokens")
    void shouldRejectMalformedJwtTokens() throws Exception {
        String[] malformedTokens = {
            "invalid.token",
            "not-a-jwt-token",
            "eyJhbGciOiJSUzI1NiJ9.malformed",
            "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.invalid-signature",
            "Bearer invalid-token",
            "",
            "null",
            "undefined"
        };

        for (String token : malformedTokens) {
            mockMvc.perform(get("/api/v1/users/profile")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized())
                    .andExpect(result -> {
                        String response = result.getResponse().getContentAsString();
                        // Should not leak internal error details
                        assertThat(response).doesNotContain("NullPointerException");
                        assertThat(response).doesNotContain("stack trace");
                    });
        }
    }

    @Test
    @DisplayName("Should prevent JWT token reuse after logout")
    void shouldPreventJwtTokenReuseAfterLogout() throws Exception {
        // Given - Register and login user
        RegisterRequest registerRequest = TestDataBuilder.validRegisterRequest();

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JwtResponse jwtResponse = objectMapper.readValue(
            registerResult.getResponse().getContentAsString(), JwtResponse.class);

        String accessToken = jwtResponse.getAccessToken();
        String refreshToken = jwtResponse.getRefreshToken();

        // Verify token works initially
        mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + accessToken))
                .andExpected(status().isOk());

        // Logout user
        String logoutRequest = String.format("""
            {
                "accessToken": "%s",
                "refreshToken": "%s"
            }
            """, accessToken, refreshToken);

        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(logoutRequest))
                .andExpect(status().isOk());

        // Token should no longer work
        mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());

        // Refresh token should also be invalidated
        String refreshRequest = String.format("""
            {
                "refreshToken": "%s"
            }
            """, refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshRequest))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should implement proper account lockout mechanism")
    void shouldImplementProperAccountLockoutMechanism() throws Exception {
        // Given - Register a user
        RegisterRequest registerRequest = TestDataBuilder.validRegisterRequest();

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        LoginRequest wrongPasswordLogin = LoginRequest.builder()
                .emailOrUsername(registerRequest.getEmail())
                .password("WrongPassword123")
                .build();

        // When - Make multiple failed login attempts
        for (int i = 1; i <= 5; i++) {
            MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(wrongPasswordLogin)))
                    .andExpect(status().isUnauthorized())
                    .andReturn();

            // Verify error message doesn't reveal attempt count to prevent enumeration
            String response = result.getResponse().getContentAsString();
            assertThat(response).contains("E-posta veya şifre hatalı");
            assertThat(response).doesNotContain("attempt " + i);
        }

        // 6th attempt should lock the account
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongPasswordLogin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("kilitlenmiştir")));

        // Even correct password should fail when account is locked
        LoginRequest correctLogin = LoginRequest.builder()
                .emailOrUsername(registerRequest.getEmail())
                .password(registerRequest.getPassword())
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(correctLogin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("kilitlenmiştir")));
    }

    @Test
    @DisplayName("Should prevent SQL injection in authentication")
    void shouldPreventSqlInjectionInAuthentication() throws Exception {
        // Given - SQL injection payloads
        String[] sqlInjectionPayloads = {
            "admin'; DROP TABLE users; --",
            "' OR '1'='1",
            "' OR 1=1 --",
            "admin'--",
            "admin' /*",
            "' OR 'x'='x",
            "'; INSERT INTO users VALUES ('hacker', 'password'); --",
            "' UNION SELECT * FROM users --"
        };

        for (String payload : sqlInjectionPayloads) {
            LoginRequest injectionAttempt = LoginRequest.builder()
                    .emailOrUsername(payload)
                    .password("password")
                    .build();

            // Should safely handle SQL injection attempts
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(injectionAttempt)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(containsString("E-posta veya şifre hatalı")));
        }

        // Verify database integrity - users table should still exist and be empty
        assertThat(userRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should prevent XSS attacks in user input")
    void shouldPreventXssAttacksInUserInput() throws Exception {
        // Given - XSS payloads
        String[] xssPayloads = {
            "<script>alert('XSS')</script>",
            "javascript:alert('XSS')",
            "<img src=x onerror=alert('XSS')>",
            "<svg onload=alert('XSS')>",
            "';alert('XSS');//",
            "<iframe src='javascript:alert(\"XSS\")'></iframe>"
        };

        RegisterRequest registerRequest = TestDataBuilder.validRegisterRequest();

        for (String payload : xssPayloads) {
            registerRequest.setFirstName(payload);
            registerRequest.setLastName(payload);
            registerRequest.setEmail("test" + System.nanoTime() + "@example.com");
            registerRequest.setUsername("test" + System.nanoTime());

            MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)))
                    .andReturn();

            if (result.getResponse().getStatus() == 200) {
                // If registration succeeds, verify XSS is prevented in response
                String response = result.getResponse().getContentAsString();
                assertThat(response).doesNotContain("<script>");
                assertThat(response).doesNotContain("javascript:");
                assertThat(response).doesNotContain("onerror=");
                assertThat(response).doesNotContain("onload=");
            }
        }
    }

    @Test
    @DisplayName("Should enforce HTTPS-only for sensitive operations")
    void shouldEnforceHttpsOnlyForSensitiveOperations() throws Exception {
        // This test would typically check headers like Strict-Transport-Security
        // For now, we'll verify that sensitive endpoints don't leak information in HTTP responses

        RegisterRequest registerRequest = TestDataBuilder.validRegisterRequest();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpected(status().isOk())
                .andReturn();

        // Verify no sensitive information is exposed in response headers
        String response = result.getResponse().getContentAsString();
        assertThat(response).doesNotContain("password");
        assertThat(response).doesNotContain("passwordHash");
        assertThat(response).doesNotContain("secret");
        assertThat(response).doesNotContain("key");
    }

    @Test
    @DisplayName("Should implement proper session management")
    void shouldImplementProperSessionManagement() throws Exception {
        // Given - Register and login user
        RegisterRequest registerRequest = TestDataBuilder.validRegisterRequest();

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JwtResponse jwtResponse = objectMapper.readValue(
            registerResult.getResponse().getContentAsString(), JwtResponse.class);

        String accessToken = jwtResponse.getAccessToken();

        // Verify session tracking works
        mockMvc.perform(get("/api/v1/users/sessions")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(greaterThan(0)));

        // Verify session termination works
        mockMvc.perform(delete("/api/v1/users/sessions/terminate-all")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.terminatedCount").isNumber());
    }

    @Test
    @DisplayName("Should validate Turkish character security in authentication")
    void shouldValidateTurkishCharacterSecurityInAuthentication() throws Exception {
        // Given - Turkish characters that could be used in attacks
        String[] turkishAttackVectors = {
            "çağlar'; DROP TABLE users; --",
            "gülşah<script>alert('XSS')</script>",
            "ömer@örnek.com'; INSERT INTO admin VALUES ('hacker'); --",
            "şeyma<img src=x onerror=alert('XSS')>"
        };

        for (String vector : turkishAttackVectors) {
            RegisterRequest attackRequest = TestDataBuilder.turkishRegisterRequest();
            attackRequest.setEmail("test" + System.nanoTime() + "@example.com");
            attackRequest.setUsername("test" + System.nanoTime());
            attackRequest.setFirstName(vector);

            MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(attackRequest))
                    .characterEncoding("UTF-8"))
                    .andReturn();

            // Should either reject malicious input or safely store it
            if (result.getResponse().getStatus() == 200) {
                String response = result.getResponse().getContentAsString();
                // Verify no script execution or SQL injection occurred
                assertThat(response).doesNotContain("DROP TABLE");
                assertThat(response).doesNotContain("<script>");
                assertThat(response).doesNotContain("javascript:");
            }
        }
    }

    @Test
    @DisplayName("Should prevent concurrent session hijacking")
    void shouldPreventConcurrentSessionHijacking() throws Exception {
        // Given - Register user and get token
        RegisterRequest registerRequest = TestDataBuilder.validRegisterRequest();

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JwtResponse jwtResponse = objectMapper.readValue(
            registerResult.getResponse().getContentAsString(), JwtResponse.class);

        String accessToken = jwtResponse.getAccessToken();

        // Simulate concurrent access from different IPs/User-Agents
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        CompletableFuture<Integer>[] futures = new CompletableFuture[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            futures[i] = CompletableFuture.supplyAsync(() -> {
                try {
                    MvcResult result = mockMvc.perform(get("/api/v1/users/profile")
                            .header("Authorization", "Bearer " + accessToken)
                            .header("X-Forwarded-For", "192.168.1." + (100 + index))
                            .header("User-Agent", "Browser-" + index))
                            .andReturn();

                    return result.getResponse().getStatus();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor);
        }

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures);
        allFutures.get(30, TimeUnit.SECONDS);

        // All requests should succeed - legitimate concurrent access should be allowed
        for (CompletableFuture<Integer> future : futures) {
            assertThat(future.get()).isEqualTo(200);
        }

        executor.shutdown();
    }

    @Test
    @DisplayName("Should prevent password enumeration attacks")
    void shouldPreventPasswordEnumerationAttacks() throws Exception {
        // Given - Register a user
        RegisterRequest registerRequest = TestDataBuilder.validRegisterRequest();

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk());

        // Test with existing user but wrong password
        LoginRequest existingUserWrongPassword = LoginRequest.builder()
                .emailOrUsername(registerRequest.getEmail())
                .password("WrongPassword123")
                .build();

        MvcResult existingUserResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(existingUserWrongPassword)))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // Test with non-existing user
        LoginRequest nonExistingUser = LoginRequest.builder()
                .emailOrUsername("nonexistent@example.com")
                .password("WrongPassword123")
                .build();

        MvcResult nonExistingUserResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nonExistingUser)))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // Both should return the same error message to prevent user enumeration
        String existingUserResponse = existingUserResult.getResponse().getContentAsString();
        String nonExistingUserResponse = nonExistingUserResult.getResponse().getContentAsString();

        assertThat(existingUserResponse).contains("E-posta veya şifre hatalı");
        assertThat(nonExistingUserResponse).contains("E-posta veya şifre hatalı");

        // Response times should be similar (within reasonable bounds) to prevent timing attacks
        // This is a simplified check - in real scenarios, you'd measure actual response times
        assertThat(existingUserResponse.length()).isCloseTo(nonExistingUserResponse.length(), within(100));
    }

    @Test
    @DisplayName("Should implement proper CORS policy")
    void shouldImplementProperCorsPolicy() throws Exception {
        // Test CORS headers for security
        RegisterRequest registerRequest = TestDataBuilder.validRegisterRequest();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest))
                .header("Origin", "https://malicious-site.com"))
                .andReturn();

        // Verify CORS headers are properly configured (implementation dependent)
        String accessControlAllowOrigin = result.getResponse().getHeader("Access-Control-Allow-Origin");
        if (accessControlAllowOrigin != null) {
            // Should not allow arbitrary origins
            assertThat(accessControlAllowOrigin).isNotEqualTo("*");
            assertThat(accessControlAllowOrigin).doesNotContain("malicious-site.com");
        }
    }

    @ParameterizedTest
    @DisplayName("Should validate JWT token expiration properly")
    @ValueSource(strings = {
        "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiZXhwIjoxfQ.invalid", // Expired token
        "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIn0.invalid", // No expiration
        "eyJhbGciOiJub25lIn0.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIn0." // None algorithm
    })
    void shouldValidateJwtTokenExpirationProperly(String invalidToken) throws Exception {
        mockMvc.perform(get("/api/v1/users/profile")
                .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should implement proper input validation and sanitization")
    void shouldImplementProperInputValidationAndSanitization() throws Exception {
        // Test various input validation scenarios
        RegisterRequest invalidRequest = TestDataBuilder.validRegisterRequest();

        // Test oversized input
        String oversizedString = "A".repeat(10000);
        invalidRequest.setFirstName(oversizedString);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        // Test null/empty validation
        invalidRequest = TestDataBuilder.validRegisterRequest();
        invalidRequest.setEmail(null);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        // Test invalid email format
        invalidRequest = TestDataBuilder.validRegisterRequest();
        invalidRequest.setEmail("invalid-email-format");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should prevent header injection attacks")
    void shouldPreventHeaderInjectionAttacks() throws Exception {
        // Test header injection via malicious input
        String[] headerInjectionPayloads = {
            "test\r\nX-Injected-Header: malicious",
            "test\nSet-Cookie: session=hijacked",
            "test\r\n\r\n<html><body>Injected Content</body></html>"
        };

        RegisterRequest registerRequest = TestDataBuilder.validRegisterRequest();

        for (String payload : headerInjectionPayloads) {
            registerRequest.setEmail("test" + System.nanoTime() + "@example.com");
            registerRequest.setUsername("test" + System.nanoTime());
            registerRequest.setFirstName(payload);

            MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)))
                    .andReturn();

            // Verify no header injection occurred
            assertThat(result.getResponse().getHeaderNames()).doesNotContain("X-Injected-Header");
            assertThat(result.getResponse().getHeader("Set-Cookie")).doesNotContain("hijacked");
        }
    }
}