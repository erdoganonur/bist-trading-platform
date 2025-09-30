package com.bisttrading.broker.integration;

import com.bisttrading.broker.config.AlgoLabProperties;
import com.bisttrading.broker.service.AlgoLabService;
import com.bisttrading.broker.util.AlgoLabEncryptionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AlgoLab Service integration tests
 * Gerçek senaryoları simüle eden end-to-end testler
 */
@SpringBootTest
@TestPropertySource(properties = {
    "algolab.api.base-url=http://localhost",
    "algolab.api.api-key=test-key",
    "algolab.api.secret-key=test-secret-key-32-characters-long",
    "algolab.timeout.connect=5s",
    "algolab.timeout.read=10s",
    "algolab.rate-limit.request-delay=100ms",
    "algolab.session.keep-alive=false"
})
class AlgoLabIntegrationTest {

    private MockWebServer mockWebServer;
    private AlgoLabService algoLabService;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Gerçek konfigürasyonla test
        AlgoLabProperties properties = new AlgoLabProperties();
        properties.getApi().setBaseUrl(mockWebServer.url("/").toString());
        properties.getApi().setApiKey("test-api-key");
        properties.getApi().setSecretKey("test-secret-key-32-characters-long");
        properties.getTimeout().setConnect(Duration.ofSeconds(5));
        properties.getTimeout().setRead(Duration.ofSeconds(10));
        properties.getRateLimit().setRequestDelay(Duration.ofMillis(100));
        properties.getSession().setKeepAlive(false);

        AlgoLabEncryptionUtil encryptionUtil = new AlgoLabEncryptionUtil(properties.getApi().getSecretKey());
        ObjectMapper objectMapper = new ObjectMapper();
        WebClient webClient = WebClient.builder()
            .baseUrl(properties.getApi().getBaseUrl())
            .build();

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
    void completeTradeWorkflow_Success() throws Exception {
        // 1. Login
        mockWebServer.enqueue(new MockResponse()
            .setBody("{"
                + "\"status\": \"success\","
                + "\"data\": {"
                    + "\"token\": \"auth-token-12345\","
                    + "\"hash\": \"auth-hash-abcdef\""
                + "}"
            + "}")
            .addHeader("Content-Type", "application/json"));

        // 2. Send Order
        mockWebServer.enqueue(new MockResponse()
            .setBody("{"
                + "\"status\": \"success\","
                + "\"message\": \"Emir başarıyla gönderildi\","
                + "\"data\": {"
                    + "\"orderId\": \"ORDER-123456\","
                    + "\"status\": \"PENDING\""
                + "}"
            + "}")
            .addHeader("Content-Type", "application/json"));

        // 3. Check Position
        mockWebServer.enqueue(new MockResponse()
            .setBody("{"
                + "\"status\": \"success\","
                + "\"data\": ["
                    + "{"
                        + "\"symbol\": \"THYAO\","
                        + "\"quantity\": 100,"
                        + "\"avgPrice\": \"15.50\","
                        + "\"currentPrice\": \"15.75\""
                    + "}"
                + "]"
            + "}")
            .addHeader("Content-Type", "application/json"));

        // Act: Complete workflow
        // 1. Login
        boolean loginResult = algoLabService.loginUser("testuser", "testpass");
        assertThat(loginResult).isTrue();

        // 2. Send order
        var orderResult = algoLabService.sendOrder(
            "THYAO", "BUY", "limit", "15.50", "100", false, false, ""
        );
        assertThat(orderResult.getStatus()).isEqualTo("success");

        // 3. Check position
        var positionResult = algoLabService.getInstantPosition("");
        assertThat(positionResult.getStatus()).isEqualTo("success");

        // Verify all requests were made
        assertThat(mockWebServer.getRequestCount()).isEqualTo(3);

        // Verify login request
        RecordedRequest loginRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(loginRequest.getPath()).isEqualTo("/api/LoginUser");
        assertThat(loginRequest.getHeader("APIKEY")).isEqualTo("test-api-key");

        // Verify order request
        RecordedRequest orderRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(orderRequest.getPath()).isEqualTo("/api/SendOrder");
        assertThat(orderRequest.getHeader("Authorization")).isEqualTo("auth-hash-abcdef");
        assertThat(orderRequest.getHeader("Checker")).isNotNull();

        // Verify position request
        RecordedRequest positionRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(positionRequest.getPath()).isEqualTo("/api/InstantPosition");
        assertThat(positionRequest.getHeader("Authorization")).isEqualTo("auth-hash-abcdef");
    }

    @Test
    void marketDataWorkflow_Success() throws Exception {
        // 1. Login
        mockWebServer.enqueue(new MockResponse()
            .setBody("{"
                + "\"status\": \"success\","
                + "\"data\": {"
                    + "\"token\": \"auth-token-12345\","
                    + "\"hash\": \"auth-hash-abcdef\""
                + "}"
            + "}")
            .addHeader("Content-Type", "application/json"));

        // 2. Get Equity Info
        mockWebServer.enqueue(new MockResponse()
            .setBody("{"
                + "\"status\": \"success\","
                + "\"data\": {"
                    + "\"symbol\": \"THYAO\","
                    + "\"name\": \"TÜRK HAVA YOLLARI\","
                    + "\"price\": \"15.50\","
                    + "\"change\": \"+0.25\","
                    + "\"volume\": \"1000000\""
                + "}"
            + "}")
            .addHeader("Content-Type", "application/json"));

        // 3. Get Candle Data
        mockWebServer.enqueue(new MockResponse()
            .setBody("{"
                + "\"status\": \"success\","
                + "\"data\": ["
                    + "{"
                        + "\"date\": \"2024-01-15\","
                        + "\"open\": \"15.25\","
                        + "\"high\": \"15.75\","
                        + "\"low\": \"15.10\","
                        + "\"close\": \"15.50\","
                        + "\"volume\": \"500000\""
                    + "}"
                + "]"
            + "}")
            .addHeader("Content-Type", "application/json"));

        // Act: Market data workflow
        // 1. Login
        boolean loginResult = algoLabService.loginUser("testuser", "testpass");
        assertThat(loginResult).isTrue();

        // 2. Get equity info
        var equityResult = algoLabService.getEquityInfo("THYAO");
        assertThat(equityResult.getStatus()).isEqualTo("success");

        // 3. Get candle data
        var candleResult = algoLabService.getCandleData("THYAO", "1D");
        assertThat(candleResult.getStatus()).isEqualTo("success");

        // Verify requests
        assertThat(mockWebServer.getRequestCount()).isEqualTo(3);
    }

    @Test
    void sessionManagement_WorksCorrectly() throws Exception {
        // 1. Login
        mockWebServer.enqueue(new MockResponse()
            .setBody("{"
                + "\"status\": \"success\","
                + "\"data\": {"
                    + "\"token\": \"auth-token-12345\","
                    + "\"hash\": \"auth-hash-abcdef\""
                + "}"
            + "}")
            .addHeader("Content-Type", "application/json"));

        // 2. Session refresh
        mockWebServer.enqueue(new MockResponse()
            .setBody("{"
                + "\"status\": \"success\","
                + "\"message\": \"Session başarıyla yenilendi\""
            + "}")
            .addHeader("Content-Type", "application/json"));

        // 3. Login control
        mockWebServer.enqueue(new MockResponse()
            .setBody("{"
                + "\"status\": \"success\","
                + "\"data\": {"
                    + "\"isLoggedIn\": true,"
                    + "\"sessionExpiry\": \"2024-01-15T16:00:00\""
                + "}"
            + "}")
            .addHeader("Content-Type", "application/json"));

        // Act: Session management workflow
        // 1. Login
        boolean loginResult = algoLabService.loginUser("testuser", "testpass");
        assertThat(loginResult).isTrue();
        assertThat(algoLabService.isAuthenticated()).isTrue();

        // 2. Refresh session
        boolean refreshResult = algoLabService.sessionRefresh();
        assertThat(refreshResult).isTrue();

        // 3. Check login status
        boolean controlResult = algoLabService.loginUserControl();
        assertThat(controlResult).isTrue();

        // Verify session state
        assertThat(algoLabService.isAlive()).isTrue(); // Should call sessionRefresh silently
    }

    @Test
    void viopOperations_Success() throws Exception {
        // 1. Login
        mockWebServer.enqueue(new MockResponse()
            .setBody("{"
                + "\"status\": \"success\","
                + "\"data\": {"
                    + "\"token\": \"auth-token-12345\","
                    + "\"hash\": \"auth-hash-abcdef\""
                + "}"
            + "}")
            .addHeader("Content-Type", "application/json"));

        // 2. VIOP Customer Overall
        mockWebServer.enqueue(new MockResponse()
            .setBody("{"
                + "\"status\": \"success\","
                + "\"data\": {"
                    + "\"balance\": \"100000.00\","
                    + "\"blockedAmount\": \"5000.00\","
                    + "\"availableBalance\": \"95000.00\""
                + "}"
            + "}")
            .addHeader("Content-Type", "application/json"));

        // 3. VIOP Customer Transactions
        mockWebServer.enqueue(new MockResponse()
            .setBody("{"
                + "\"status\": \"success\","
                + "\"data\": ["
                    + "{"
                        + "\"transactionId\": \"TXN-123456\","
                        + "\"date\": \"2024-01-15\","
                        + "\"amount\": \"1000.00\","
                        + "\"type\": \"BUY\""
                    + "}"
                + "]"
            + "}")
            .addHeader("Content-Type", "application/json"));

        // Act: VIOP operations
        // 1. Login
        boolean loginResult = algoLabService.loginUser("testuser", "testpass");
        assertThat(loginResult).isTrue();

        // 2. Get VIOP customer overall
        var overallResult = algoLabService.getViopCustomerOverall("");
        assertThat(overallResult.getStatus()).isEqualTo("success");

        // 3. Get VIOP transactions
        var transactionsResult = algoLabService.getViopCustomerTransactions("");
        assertThat(transactionsResult.getStatus()).isEqualTo("success");

        // Verify requests
        assertThat(mockWebServer.getRequestCount()).isEqualTo(3);

        // Verify VIOP requests
        mockWebServer.takeRequest(1, TimeUnit.SECONDS); // Login request

        RecordedRequest overallRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(overallRequest.getPath()).isEqualTo("/api/ViopCustomerOverall");

        RecordedRequest transactionsRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertThat(transactionsRequest.getPath()).isEqualTo("/api/ViopCustomerTransactions");
    }

    @Test
    void orderHistory_WorksCorrectly() throws Exception {
        // 1. Login
        mockWebServer.enqueue(new MockResponse()
            .setBody("{"
                + "\"status\": \"success\","
                + "\"data\": {"
                    + "\"token\": \"auth-token-12345\","
                    + "\"hash\": \"auth-hash-abcdef\""
                + "}"
            + "}")
            .addHeader("Content-Type", "application/json"));

        // 2. Equity Order History
        mockWebServer.enqueue(new MockResponse()
            .setBody("{"
                + "\"status\": \"success\","
                + "\"data\": ["
                    + "{"
                        + "\"orderId\": \"ORDER-123456\","
                        + "\"symbol\": \"THYAO\","
                        + "\"side\": \"BUY\","
                        + "\"quantity\": 100,"
                        + "\"price\": \"15.50\","
                        + "\"status\": \"FILLED\""
                    + "}"
                + "]"
            + "}")
            .addHeader("Content-Type", "application/json"));

        // 3. VIOP Order History
        mockWebServer.enqueue(new MockResponse()
            .setBody("{"
                + "\"status\": \"success\","
                + "\"data\": ["
                    + "{"
                        + "\"orderId\": \"VIOP-789012\","
                        + "\"symbol\": \"XU030\","
                        + "\"side\": \"SELL\","
                        + "\"quantity\": 5,"
                        + "\"price\": \"1500.00\","
                        + "\"status\": \"CANCELLED\""
                    + "}"
                + "]"
            + "}")
            .addHeader("Content-Type", "application/json"));

        // Act: Order history workflow
        // 1. Login
        boolean loginResult = algoLabService.loginUser("testuser", "testpass");
        assertThat(loginResult).isTrue();

        // 2. Get equity order history
        var equityHistoryResult = algoLabService.getEquityOrderHistory("ORDER-123456", "");
        assertThat(equityHistoryResult.getStatus()).isEqualTo("success");

        // 3. Get VIOP order history
        var viopHistoryResult = algoLabService.getViopOrderHistory("VIOP-789012", "");
        assertThat(viopHistoryResult.getStatus()).isEqualTo("success");

        // Verify requests
        assertThat(mockWebServer.getRequestCount()).isEqualTo(3);
    }
}