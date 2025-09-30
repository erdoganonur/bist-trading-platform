package com.bisttrading.broker.service;

import com.bisttrading.broker.config.AlgoLabProperties;
import com.bisttrading.broker.dto.AlgoLabResponse;
import com.bisttrading.broker.exception.AlgoLabException;
import com.bisttrading.broker.util.AlgoLabEncryptionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * AlgoLabService için kapsamlı test suite
 */
@ExtendWith(MockitoExtension.class)
class AlgoLabServiceTest {

    private MockWebServer mockWebServer;
    private AlgoLabService algoLabService;
    private AlgoLabProperties properties;
    private ObjectMapper objectMapper;

    @Mock
    private AlgoLabEncryptionUtil encryptionUtil;

    @BeforeEach
    void setUp() throws Exception {
        // Mock web server başlat
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Configuration setup
        properties = new AlgoLabProperties();
        properties.getApi().setBaseUrl(mockWebServer.url("/").toString());
        properties.getApi().setApiKey("test-api-key");
        properties.getApi().setSecretKey("test-secret-key");
        properties.getTimeout().setConnect(Duration.ofSeconds(5));
        properties.getTimeout().setRead(Duration.ofSeconds(10));
        properties.getRateLimit().setRequestDelay(Duration.ofMillis(100)); // Test için kısa delay

        objectMapper = new ObjectMapper();

        // WebClient konfigürasyonu
        WebClient webClient = WebClient.builder()
            .baseUrl(properties.getApi().getBaseUrl())
            .build();

        // Service instance oluştur
        algoLabService = new AlgoLabService(properties, encryptionUtil, objectMapper, webClient);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mockWebServer != null) {
            mockWebServer.shutdown();
        }
        if (algoLabService != null) {
            algoLabService.destroy();
        }
    }

    @Test
    void loginUser_Success() throws Exception {
        // Arrange
        String expectedResponse = "{"
            + "\"status\": \"success\","
            + "\"message\": \"Giriş başarılı\","
            + "\"data\": {"
                + "\"token\": \"test-token\","
                + "\"hash\": \"test-hash\""
            + "}"
            + "}";

        mockWebServer.enqueue(new MockResponse()
            .setBody(expectedResponse)
            .addHeader("Content-Type", "application/json"));

        when(encryptionUtil.encrypt(any(), any())).thenReturn("encrypted-password");

        // Act
        boolean result = algoLabService.loginUser("testuser", "testpass");

        // Assert
        assertThat(result).isTrue();
        assertThat(algoLabService.isAuthenticated()).isTrue();
        assertThat(algoLabService.getToken()).isEqualTo("test-token");
        assertThat(algoLabService.getHash()).isEqualTo("test-hash");

        // Request doğrulama
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).isEqualTo("/api/LoginUser");
        assertThat(request.getHeader("APIKEY")).isEqualTo("test-api-key");
    }

    @Test
    void loginUser_Failure() {
        // Arrange
        String errorResponse = "{"
            + "\"status\": \"error\","
            + "\"message\": \"Kullanıcı adı veya şifre hatalı\""
            + "}";

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(401)
            .setBody(errorResponse)
            .addHeader("Content-Type", "application/json"));

        when(encryptionUtil.encrypt(any(), any())).thenReturn("encrypted-password");

        // Act
        boolean result = algoLabService.loginUser("wronguser", "wrongpass");

        // Assert
        assertThat(result).isFalse();
        assertThat(algoLabService.isAuthenticated()).isFalse();
    }

    @Test
    void sendOrder_Success() throws Exception {
        // Önce login yap
        setupSuccessfulLogin();

        // Order response
        String orderResponse = "{"
            + "\"status\": \"success\","
            + "\"message\": \"Emir başarıyla gönderildi\","
            + "\"data\": {"
                + "\"orderId\": \"12345\","
                + "\"status\": \"PENDING\""
            + "}"
            + "}";

        mockWebServer.enqueue(new MockResponse()
            .setBody(orderResponse)
            .addHeader("Content-Type", "application/json"));

        when(encryptionUtil.makeChecker(any(), any(), any(), any())).thenReturn("test-checker");

        // Act
        AlgoLabResponse<Object> result = algoLabService.sendOrder(
            "THYAO", "BUY", "limit", "15.50", "100", false, false, ""
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("success");

        // Request doğrulama
        RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS); // Login request
        assertThat(request).isNotNull();

        request = mockWebServer.takeRequest(1, TimeUnit.SECONDS); // Order request
        assertThat(request.getPath()).isEqualTo("/api/SendOrder");
        assertThat(request.getHeader("Authorization")).isEqualTo("test-hash");
        assertThat(request.getHeader("Checker")).isEqualTo("test-checker");
    }

    @Test
    void sendOrder_NotAuthenticated() {
        // Act & Assert
        assertThatThrownBy(() ->
            algoLabService.sendOrder("THYAO", "BUY", "limit", "15.50", "100", false, false, "")
        )
        .isInstanceOf(AlgoLabException.class)
        .hasMessageContaining("Authentication yapılmamış");
    }

    @Test
    void sessionRefresh_Success() throws Exception {
        // Önce login yap
        setupSuccessfulLogin();

        // Session refresh response
        String refreshResponse = "{"
            + "\"status\": \"success\","
            + "\"message\": \"Session yenilendi\""
            + "}";

        mockWebServer.enqueue(new MockResponse()
            .setBody(refreshResponse)
            .addHeader("Content-Type", "application/json"));

        when(encryptionUtil.makeChecker(any(), any(), any(), any())).thenReturn("test-checker");

        // Act
        boolean result = algoLabService.sessionRefresh();

        // Assert
        assertThat(result).isTrue();

        // Request doğrulama
        RecordedRequest loginRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(loginRequest).isNotNull();

        RecordedRequest refreshRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(refreshRequest.getPath()).isEqualTo("/api/SessionRefresh");
        assertThat(refreshRequest.getHeader("Authorization")).isEqualTo("test-hash");
    }

    @Test
    void getEquityInfo_Success() throws Exception {
        // Önce login yap
        setupSuccessfulLogin();

        // Market data response
        String marketDataResponse = "{"
            + "\"status\": \"success\","
            + "\"data\": {"
                + "\"symbol\": \"THYAO\","
                + "\"price\": \"15.50\","
                + "\"volume\": \"1000000\""
            + "}"
            + "}";

        mockWebServer.enqueue(new MockResponse()
            .setBody(marketDataResponse)
            .addHeader("Content-Type", "application/json"));

        when(encryptionUtil.makeChecker(any(), any(), any(), any())).thenReturn("test-checker");

        // Act
        AlgoLabResponse<Object> result = algoLabService.getEquityInfo("THYAO");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("success");

        // Request doğrulama - login request
        RecordedRequest loginRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(loginRequest).isNotNull();

        // Market data request
        RecordedRequest request = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request.getPath()).isEqualTo("/api/GetEquityInfo");
    }

    @Test
    void rateLimit_EnforcesDelay() throws Exception {
        // Önce login yap
        setupSuccessfulLogin();

        // İki ardışık request için response'lar
        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"status\": \"success\"}")
            .addHeader("Content-Type", "application/json"));

        mockWebServer.enqueue(new MockResponse()
            .setBody("{\"status\": \"success\"}")
            .addHeader("Content-Type", "application/json"));

        when(encryptionUtil.makeChecker(any(), any(), any(), any())).thenReturn("test-checker");

        // Act - İki ardışık request yap
        long startTime = System.currentTimeMillis();
        algoLabService.getEquityInfo("THYAO");
        algoLabService.getEquityInfo("GARAN");
        long endTime = System.currentTimeMillis();

        // Assert - Rate limit delay uygulandığını kontrol et
        long duration = endTime - startTime;
        assertThat(duration).isGreaterThan(90); // 100ms delay - tolerance için 90ms
    }

    @Test
    void circuitBreaker_OpensOnFailures() throws Exception {
        // Önce login yap
        setupSuccessfulLogin();

        // Consecutive failures için 500 responses
        for (int i = 0; i < 6; i++) { // Circuit breaker açmak için yeterli hata
            mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("{\"error\": \"Server error\"}")
                .addHeader("Content-Type", "application/json"));
        }

        when(encryptionUtil.makeChecker(any(), any(), any(), any())).thenReturn("test-checker");

        // Act - Multiple failing requests
        for (int i = 0; i < 6; i++) {
            try {
                algoLabService.getEquityInfo("THYAO");
            } catch (Exception e) {
                // Expected failures
            }
        }

        // Circuit breaker açık durumda olduğunu test et
        // Not: Bu gerçek implementasyonda CallNotPermittedException fırlatacak
        assertThatThrownBy(() -> algoLabService.getEquityInfo("TEST"))
            .hasMessageContaining("Circuit breaker"); // Mesaj circuit breaker ile ilgili olmalı
    }

    @Test
    void keepAlive_WorksCorrectly() throws Exception {
        // Keep-alive interval'i test için kısa ayarla
        properties.getSession().setKeepAliveInterval(Duration.ofSeconds(1));
        properties.getSession().setKeepAlive(true);

        // Login response
        setupSuccessfulLogin();

        // Keep-alive için session refresh responses
        for (int i = 0; i < 3; i++) {
            mockWebServer.enqueue(new MockResponse()
                .setBody("{\"status\": \"success\"}")
                .addHeader("Content-Type", "application/json"));
        }

        when(encryptionUtil.makeChecker(any(), any(), any(), any())).thenReturn("test-checker");

        // Login yap
        algoLabService.loginUser("testuser", "testpass");

        // Keep-alive'ın çalıştığını bekle
        await()
            .atMost(5, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                assertThat(mockWebServer.getRequestCount()).isGreaterThan(1);
            });
    }

    private void setupSuccessfulLogin() {
        String loginResponse = "{"
            + "\"status\": \"success\","
            + "\"data\": {"
                + "\"token\": \"test-token\","
                + "\"hash\": \"test-hash\""
            + "}"
            + "}";

        mockWebServer.enqueue(new MockResponse()
            .setBody(loginResponse)
            .addHeader("Content-Type", "application/json"));

        when(encryptionUtil.encrypt(any(), any())).thenReturn("encrypted-password");
    }
}