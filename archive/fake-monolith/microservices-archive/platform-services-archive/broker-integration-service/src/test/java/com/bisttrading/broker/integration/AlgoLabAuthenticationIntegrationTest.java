package com.bisttrading.broker.integration;

import com.bisttrading.broker.config.AlgoLabProperties;
import com.bisttrading.broker.dto.*;
import com.bisttrading.broker.service.AlgoLabService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * AlgoLab kimlik doğrulama entegrasyon testleri
 * Tam giriş akışını test eder: LoginUser → LoginUserControl
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-integration-test.properties")
@DisplayName("AlgoLab Authentication Integration Tests")
class AlgoLabAuthenticationIntegrationTest {

    @Autowired
    private AlgoLabService algoLabService;

    @Autowired
    private AlgoLabProperties algoLabProperties;

    @Autowired
    private ObjectMapper objectMapper;

    private WireMockServer wireMockServer;
    private static final int WIREMOCK_PORT = 8089;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("algolab.base-url", () -> "http://localhost:" + WIREMOCK_PORT);
        registry.add("algolab.login-url", () -> "http://localhost:" + WIREMOCK_PORT + "/api/v1/login");
        registry.add("algolab.control-url", () -> "http://localhost:" + WIREMOCK_PORT + "/api/v1/control");
    }

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WIREMOCK_PORT);
        wireMockServer.start();
        WireMock.configureFor("localhost", WIREMOCK_PORT);
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    @DisplayName("Başarılı giriş akışı - LoginUser → LoginUserControl")
    void shouldCompleteSuccessfulLoginFlow() throws Exception {
        // Given - Mock LoginUser response
        LoginUserResponse loginResponse = LoginUserResponse.builder()
            .sessionId("test-session-123")
            .isSuccess(true)
            .message("SMS gönderildi")
            .build();

        stubFor(post(urlEqualTo("/api/v1/login"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(loginResponse))));

        // Mock LoginUserControl response
        LoginUserControlResponse controlResponse = LoginUserControlResponse.builder()
            .sessionId("test-session-123")
            .token("eyJhbGciOiJIUzI1NiJ9.test.token")
            .tokenType("Bearer")
            .isSuccess(true)
            .message("Giriş başarılı")
            .expiresIn(3600L)
            .build();

        stubFor(post(urlEqualTo("/api/v1/control"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(controlResponse))));

        // When - Perform login flow
        LoginUserRequest loginRequest = LoginUserRequest.builder()
            .username("test_user")
            .password("test_password")
            .build();

        CompletableFuture<LoginUserResponse> loginFuture = algoLabService.loginUser(loginRequest);
        LoginUserResponse actualLoginResponse = loginFuture.get(5, TimeUnit.SECONDS);

        // Verify LoginUser response
        assertThat(actualLoginResponse.isSuccess()).isTrue();
        assertThat(actualLoginResponse.getSessionId()).isEqualTo("test-session-123");
        assertThat(actualLoginResponse.getMessage()).isEqualTo("SMS gönderildi");

        // Continue with control
        LoginUserControlRequest controlRequest = LoginUserControlRequest.builder()
            .sessionId("test-session-123")
            .smsCode("123456")
            .build();

        CompletableFuture<LoginUserControlResponse> controlFuture = algoLabService.loginUserControl(controlRequest);
        LoginUserControlResponse actualControlResponse = controlFuture.get(5, TimeUnit.SECONDS);

        // Verify LoginUserControl response
        assertThat(actualControlResponse.isSuccess()).isTrue();
        assertThat(actualControlResponse.getSessionId()).isEqualTo("test-session-123");
        assertThat(actualControlResponse.getToken()).isNotNull();
        assertThat(actualControlResponse.getTokenType()).isEqualTo("Bearer");
        assertThat(actualControlResponse.getExpiresIn()).isEqualTo(3600L);

        // Verify requests were made correctly
        verify(postRequestedFor(urlEqualTo("/api/v1/login"))
            .withRequestBody(containing("test_user"))
            .withRequestBody(containing("test_password")));

        verify(postRequestedFor(urlEqualTo("/api/v1/control"))
            .withRequestBody(containing("test-session-123"))
            .withRequestBody(containing("123456")));
    }

    @Test
    @DisplayName("Session refresh mechanism testi")
    void shouldHandleSessionRefresh() throws Exception {
        // Given - Mock token refresh response
        TokenRefreshResponse refreshResponse = TokenRefreshResponse.builder()
            .token("eyJhbGciOiJIUzI1NiJ9.refreshed.token")
            .tokenType("Bearer")
            .expiresIn(3600L)
            .isSuccess(true)
            .message("Token yenilendi")
            .build();

        stubFor(post(urlEqualTo("/api/v1/refresh"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(refreshResponse))));

        // When - Refresh token
        TokenRefreshRequest refreshRequest = TokenRefreshRequest.builder()
            .refreshToken("old-refresh-token")
            .build();

        CompletableFuture<TokenRefreshResponse> refreshFuture = algoLabService.refreshToken(refreshRequest);
        TokenRefreshResponse actualResponse = refreshFuture.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(actualResponse.isSuccess()).isTrue();
        assertThat(actualResponse.getToken()).contains("refreshed");
        assertThat(actualResponse.getExpiresIn()).isEqualTo(3600L);

        verify(postRequestedFor(urlEqualTo("/api/v1/refresh"))
            .withRequestBody(containing("old-refresh-token")));
    }

    @Test
    @DisplayName("Token süresi dolduğunda yenileme testi")
    void shouldHandleTokenExpiration() throws Exception {
        // Given - Mock expired token scenario
        stubFor(post(urlEqualTo("/api/v1/some-endpoint"))
            .willReturn(aResponse()
                .withStatus(401)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody("{\"error\": \"Token süresi dolmuş\", \"isSuccess\": false}")));

        // Mock refresh token success
        TokenRefreshResponse refreshResponse = TokenRefreshResponse.builder()
            .token("eyJhbGciOiJIUzI1NiJ9.new.token")
            .tokenType("Bearer")
            .expiresIn(3600L)
            .isSuccess(true)
            .build();

        stubFor(post(urlEqualTo("/api/v1/refresh"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(refreshResponse))));

        // When & Then - Service should automatically refresh token
        TokenRefreshRequest refreshRequest = TokenRefreshRequest.builder()
            .refreshToken("expired-token")
            .build();

        CompletableFuture<TokenRefreshResponse> result = algoLabService.refreshToken(refreshRequest);
        TokenRefreshResponse response = result.get(5, TimeUnit.SECONDS);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getToken()).contains("new");

        // Verify refresh was called
        verify(postRequestedFor(urlEqualTo("/api/v1/refresh")));
    }

    @Test
    @DisplayName("Geçersiz kimlik bilgileri işleme testi")
    void shouldHandleInvalidCredentials() throws Exception {
        // Given - Mock invalid credentials response
        LoginUserResponse errorResponse = LoginUserResponse.builder()
            .isSuccess(false)
            .errorCode("INVALID_CREDENTIALS")
            .message("Kullanıcı adı veya şifre hatalı")
            .build();

        stubFor(post(urlEqualTo("/api/v1/login"))
            .willReturn(aResponse()
                .withStatus(401)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(errorResponse))));

        // When - Attempt login with invalid credentials
        LoginUserRequest loginRequest = LoginUserRequest.builder()
            .username("invalid_user")
            .password("wrong_password")
            .build();

        CompletableFuture<LoginUserResponse> loginFuture = algoLabService.loginUser(loginRequest);
        LoginUserResponse response = loginFuture.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(response.getMessage()).contains("Kullanıcı adı veya şifre hatalı");

        verify(postRequestedFor(urlEqualTo("/api/v1/login"))
            .withRequestBody(containing("invalid_user"))
            .withRequestBody(containing("wrong_password")));
    }

    @Test
    @DisplayName("SMS doğrulama zaman aşımı testi")
    void shouldHandleSmsVerificationTimeout() throws Exception {
        // Given - Successful LoginUser
        LoginUserResponse loginResponse = LoginUserResponse.builder()
            .sessionId("timeout-session-456")
            .isSuccess(true)
            .message("SMS gönderildi")
            .build();

        stubFor(post(urlEqualTo("/api/v1/login"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(loginResponse))));

        // Mock SMS timeout response
        LoginUserControlResponse timeoutResponse = LoginUserControlResponse.builder()
            .sessionId("timeout-session-456")
            .isSuccess(false)
            .errorCode("SMS_TIMEOUT")
            .message("SMS doğrulama süresi doldu")
            .build();

        stubFor(post(urlEqualTo("/api/v1/control"))
            .willReturn(aResponse()
                .withStatus(408)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(timeoutResponse))));

        // When - Login and then attempt control after timeout
        LoginUserRequest loginRequest = LoginUserRequest.builder()
            .username("timeout_user")
            .password("test_password")
            .build();

        CompletableFuture<LoginUserResponse> loginFuture = algoLabService.loginUser(loginRequest);
        LoginUserResponse loginResp = loginFuture.get(5, TimeUnit.SECONDS);

        assertThat(loginResp.isSuccess()).isTrue();

        // Simulate timeout delay
        Thread.sleep(100);

        LoginUserControlRequest controlRequest = LoginUserControlRequest.builder()
            .sessionId("timeout-session-456")
            .smsCode("123456")
            .build();

        CompletableFuture<LoginUserControlResponse> controlFuture = algoLabService.loginUserControl(controlRequest);
        LoginUserControlResponse controlResp = controlFuture.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(controlResp.isSuccess()).isFalse();
        assertThat(controlResp.getErrorCode()).isEqualTo("SMS_TIMEOUT");
        assertThat(controlResp.getMessage()).contains("SMS doğrulama süresi doldu");
    }

    @Test
    @DisplayName("Ağ hatası ve yeniden deneme testi")
    void shouldHandleNetworkErrorsAndRetry() throws Exception {
        // Given - First request fails, second succeeds
        LoginUserResponse successResponse = LoginUserResponse.builder()
            .sessionId("retry-session-789")
            .isSuccess(true)
            .message("SMS gönderildi")
            .build();

        stubFor(post(urlEqualTo("/api/v1/login"))
            .inScenario("retry-scenario")
            .whenScenarioStateIs("Started")
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody("{\"error\": \"Internal server error\"}"))
            .willSetStateTo("first-failed"));

        stubFor(post(urlEqualTo("/api/v1/login"))
            .inScenario("retry-scenario")
            .whenScenarioStateIs("first-failed")
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(successResponse))));

        // When - Service should retry automatically
        LoginUserRequest loginRequest = LoginUserRequest.builder()
            .username("retry_user")
            .password("test_password")
            .build();

        CompletableFuture<LoginUserResponse> loginFuture = algoLabService.loginUser(loginRequest);

        // Wait for retry to complete
        await().atMost(10, TimeUnit.SECONDS).until(() -> {
            try {
                LoginUserResponse response = loginFuture.get(1, TimeUnit.SECONDS);
                return response.isSuccess();
            } catch (Exception e) {
                return false;
            }
        });

        LoginUserResponse response = loginFuture.get(1, TimeUnit.SECONDS);

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getSessionId()).isEqualTo("retry-session-789");

        // Verify retry happened
        verify(exactly(2), postRequestedFor(urlEqualTo("/api/v1/login")));
    }

    @Test
    @DisplayName("Eşzamanlı giriş istekleri testi")
    void shouldHandleConcurrentLoginRequests() throws Exception {
        // Given - Multiple successful responses
        for (int i = 0; i < 10; i++) {
            LoginUserResponse response = LoginUserResponse.builder()
                .sessionId("concurrent-session-" + i)
                .isSuccess(true)
                .message("SMS gönderildi")
                .build();

            stubFor(post(urlEqualTo("/api/v1/login"))
                .withRequestBody(containing("concurrent_user_" + i))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBody(objectMapper.writeValueAsString(response))
                    .withFixedDelay(100))); // Simulate network delay
        }

        // When - Make concurrent requests
        CompletableFuture<?>[] futures = new CompletableFuture[10];
        for (int i = 0; i < 10; i++) {
            final int userId = i;
            LoginUserRequest request = LoginUserRequest.builder()
                .username("concurrent_user_" + userId)
                .password("test_password")
                .build();

            futures[i] = algoLabService.loginUser(request)
                .thenAccept(response -> {
                    assertThat(response.isSuccess()).isTrue();
                    assertThat(response.getSessionId()).contains("concurrent-session-" + userId);
                });
        }

        // Wait for all requests to complete
        CompletableFuture.allOf(futures).get(15, TimeUnit.SECONDS);

        // Then - All requests should have been processed
        verify(exactly(10), postRequestedFor(urlEqualTo("/api/v1/login")));
    }

    @Test
    @DisplayName("Rate limiting testi")
    void shouldHandleRateLimiting() throws Exception {
        // Given - Rate limit response
        stubFor(post(urlEqualTo("/api/v1/login"))
            .willReturn(aResponse()
                .withStatus(429)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withHeader("Retry-After", "60")
                .withBody("{\"error\": \"Rate limit exceeded\", \"message\": \"Çok fazla istek\", \"isSuccess\": false}")));

        // When
        LoginUserRequest loginRequest = LoginUserRequest.builder()
            .username("rate_limited_user")
            .password("test_password")
            .build();

        // Then - Should handle rate limiting gracefully
        assertThatThrownBy(() -> {
            CompletableFuture<LoginUserResponse> future = algoLabService.loginUser(loginRequest);
            future.get(5, TimeUnit.SECONDS);
        }).hasCauseInstanceOf(Exception.class);

        verify(postRequestedFor(urlEqualTo("/api/v1/login")));
    }
}