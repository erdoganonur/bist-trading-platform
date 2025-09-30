package com.bisttrading.broker.e2e;

import com.bisttrading.broker.config.AlgoLabProperties;
import com.bisttrading.broker.dto.*;
import com.bisttrading.broker.service.AlgoLabService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * AlgoLab End-to-End entegrasyon testleri
 * Tam trading akış senaryolarını test eder: Authentication → Market Data → Order Management
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-integration-test.properties")
@DisplayName("AlgoLab End-to-End Integration Tests")
class AlgoLabEndToEndIntegrationTest {

    @Autowired
    private AlgoLabService algoLabService;

    @Autowired
    private AlgoLabProperties algoLabProperties;

    @Autowired
    private ObjectMapper objectMapper;

    private WireMockServer wireMockServer;
    private static final int WIREMOCK_PORT = 8095;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("algolab.base-url", () -> "http://localhost:" + WIREMOCK_PORT);
        registry.add("algolab.login-url", () -> "http://localhost:" + WIREMOCK_PORT + "/api/v1/login");
        registry.add("algolab.control-url", () -> "http://localhost:" + WIREMOCK_PORT + "/api/v1/control");
        registry.add("algolab.order-url", () -> "http://localhost:" + WIREMOCK_PORT + "/api/v1/orders");
        registry.add("algolab.market-data-url", () -> "http://localhost:" + WIREMOCK_PORT + "/api/v1/market-data");
        registry.add("algolab.cancel-url", () -> "http://localhost:" + WIREMOCK_PORT + "/api/v1/orders/cancel");
        registry.add("algolab.status-url", () -> "http://localhost:" + WIREMOCK_PORT + "/api/v1/orders/status");
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
    @DisplayName("Complete Trading Session - Login to Order Execution")
    void shouldCompleteFullTradingSession() throws Exception {
        // Given - Setup complete trading session scenario
        setupAuthenticationMocks();
        setupMarketDataMocks();
        setupOrderManagementMocks();

        // STEP 1: Authentication Flow
        log.info("=== STEP 1: Starting Authentication Flow ===");

        LoginUserRequest loginRequest = LoginUserRequest.builder()
            .username("e2e_trader")
            .password("secure_password")
            .build();

        CompletableFuture<LoginUserResponse> loginFuture = algoLabService.loginUser(loginRequest);
        LoginUserResponse loginResponse = loginFuture.get(10, TimeUnit.SECONDS);

        assertThat(loginResponse.isSuccess()).isTrue();
        assertThat(loginResponse.getSessionId()).isNotNull();
        log.info("Login successful, session ID: {}", loginResponse.getSessionId());

        // STEP 2: SMS Control
        log.info("=== STEP 2: SMS Verification ===");

        LoginUserControlRequest controlRequest = LoginUserControlRequest.builder()
            .sessionId(loginResponse.getSessionId())
            .smsCode("123456")
            .build();

        CompletableFuture<LoginUserControlResponse> controlFuture = algoLabService.loginUserControl(controlRequest);
        LoginUserControlResponse controlResponse = controlFuture.get(10, TimeUnit.SECONDS);

        assertThat(controlResponse.isSuccess()).isTrue();
        assertThat(controlResponse.getToken()).isNotNull();
        log.info("SMS verification successful, received token");

        // STEP 3: Market Data Retrieval
        log.info("=== STEP 3: Market Data Retrieval ===");

        MarketDataRequest marketDataRequest = MarketDataRequest.builder()
            .symbol("AKBNK")
            .build();

        CompletableFuture<MarketDataResponse> marketDataFuture = algoLabService.getMarketData(marketDataRequest);
        MarketDataResponse marketData = marketDataFuture.get(10, TimeUnit.SECONDS);

        assertThat(marketData.isSuccess()).isTrue();
        assertThat(marketData.getLastPrice()).isNotNull();
        log.info("Market data retrieved: {} @ {}", marketData.getSymbol(), marketData.getLastPrice());

        // STEP 4: Order Placement
        log.info("=== STEP 4: Order Placement ===");

        PlaceOrderRequest orderRequest = PlaceOrderRequest.builder()
            .symbol("AKBNK")
            .side("BUY")
            .orderType("LIMIT")
            .quantity(1000L)
            .price(marketData.getLastPrice().subtract(new BigDecimal("0.05"))) // Slight discount
            .timeInForce("GTC")
            .build();

        CompletableFuture<PlaceOrderResponse> orderFuture = algoLabService.placeOrder(orderRequest);
        PlaceOrderResponse orderResponse = orderFuture.get(10, TimeUnit.SECONDS);

        assertThat(orderResponse.isSuccess()).isTrue();
        assertThat(orderResponse.getOrderId()).isNotNull();
        log.info("Order placed successfully, order ID: {}", orderResponse.getOrderId());

        // STEP 5: Order Status Tracking
        log.info("=== STEP 5: Order Status Tracking ===");

        OrderStatusRequest statusRequest = OrderStatusRequest.builder()
            .orderId(orderResponse.getOrderId())
            .symbol("AKBNK")
            .build();

        CompletableFuture<OrderStatusResponse> statusFuture = algoLabService.getOrderStatus(statusRequest);
        OrderStatusResponse statusResponse = statusFuture.get(10, TimeUnit.SECONDS);

        assertThat(statusResponse.isSuccess()).isTrue();
        assertThat(statusResponse.getOrderId()).isEqualTo(orderResponse.getOrderId());
        log.info("Order status: {} - {}", statusResponse.getStatus(), statusResponse.getOrderId());

        // STEP 6: Partial Fill Scenario and Order Update
        log.info("=== STEP 6: Order Management ===");

        if ("PARTIALLY_FILLED".equals(statusResponse.getStatus())) {
            // Update order if partially filled
            UpdateOrderRequest updateRequest = UpdateOrderRequest.builder()
                .orderId(orderResponse.getOrderId())
                .symbol("AKBNK")
                .quantity(statusResponse.getRemainingQuantity())
                .price(marketData.getLastPrice()) // Match current market price
                .build();

            CompletableFuture<UpdateOrderResponse> updateFuture = algoLabService.updateOrder(updateRequest);
            UpdateOrderResponse updateResponse = updateFuture.get(10, TimeUnit.SECONDS);

            assertThat(updateResponse.isSuccess()).isTrue();
            log.info("Order updated successfully");
        }

        // STEP 7: Final Order Cancellation (if still active)
        log.info("=== STEP 7: Order Cancellation ===");

        CancelOrderRequest cancelRequest = CancelOrderRequest.builder()
            .orderId(orderResponse.getOrderId())
            .symbol("AKBNK")
            .build();

        CompletableFuture<CancelOrderResponse> cancelFuture = algoLabService.cancelOrder(cancelRequest);
        CancelOrderResponse cancelResponse = cancelFuture.get(10, TimeUnit.SECONDS);

        // Cancel might fail if order already filled - that's okay
        if (cancelResponse.isSuccess()) {
            log.info("Order cancelled successfully");
        } else {
            log.info("Order cancellation failed (possibly already filled): {}", cancelResponse.getMessage());
        }

        // Verify complete session flow
        verify(postRequestedFor(urlEqualTo("/api/v1/login")));
        verify(postRequestedFor(urlEqualTo("/api/v1/control")));
        verify(getRequestedFor(urlMatching("/api/v1/market-data.*")));
        verify(postRequestedFor(urlEqualTo("/api/v1/orders")));
        verify(getRequestedFor(urlMatching("/api/v1/orders/status.*")));

        log.info("=== E2E Trading Session Completed Successfully ===");
    }

    @Test
    @DisplayName("High-Frequency Trading Scenario")
    void shouldHandleHighFrequencyTradingScenario() throws Exception {
        // Given - Setup for HFT scenario
        setupAuthenticationMocks();
        setupMarketDataMocks();
        setupHighFrequencyOrderMocks();

        // Authentication first
        LoginUserResponse loginResponse = authenticateUser();
        LoginUserControlResponse controlResponse = verifyWithSMS(loginResponse.getSessionId());

        assertThat(controlResponse.isSuccess()).isTrue();

        // When - Execute high-frequency trading
        String[] symbols = {"AKBNK_HFT", "THYAO_HFT", "GARAN_HFT"};
        int ordersPerSymbol = 10;
        CountDownLatch hftLatch = new CountDownLatch(symbols.length * ordersPerSymbol);
        AtomicInteger successfulOrders = new AtomicInteger(0);
        AtomicInteger failedOrders = new AtomicInteger(0);

        List<CompletableFuture<Void>> hftFutures = new ArrayList<>();

        for (String symbol : symbols) {
            CompletableFuture<Void> symbolFuture = CompletableFuture.runAsync(() -> {
                for (int i = 0; i < ordersPerSymbol; i++) {
                    try {
                        // Get current market data
                        MarketDataRequest marketDataRequest = MarketDataRequest.builder()
                            .symbol(symbol)
                            .build();

                        MarketDataResponse marketData = algoLabService.getMarketData(marketDataRequest)
                            .get(5, TimeUnit.SECONDS);

                        if (marketData.isSuccess()) {
                            // Place rapid orders
                            PlaceOrderRequest orderRequest = PlaceOrderRequest.builder()
                                .symbol(symbol)
                                .side(i % 2 == 0 ? "BUY" : "SELL")
                                .orderType("LIMIT")
                                .quantity(100L + (i * 10))
                                .price(marketData.getLastPrice().add(new BigDecimal(String.valueOf((i % 5) * 0.01))))
                                .timeInForce("IOC")
                                .build();

                            PlaceOrderResponse orderResponse = algoLabService.placeOrder(orderRequest)
                                .get(5, TimeUnit.SECONDS);

                            if (orderResponse.isSuccess()) {
                                successfulOrders.incrementAndGet();
                                log.debug("HFT order {} successful for {}", i + 1, symbol);
                            } else {
                                failedOrders.incrementAndGet();
                                log.warn("HFT order {} failed for {}: {}", i + 1, symbol, orderResponse.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        failedOrders.incrementAndGet();
                        log.error("HFT order error for {}: {}", symbol, e.getMessage());
                    } finally {
                        hftLatch.countDown();
                    }

                    // Brief pause to simulate realistic HFT timing
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });

            hftFutures.add(symbolFuture);
        }

        // Wait for completion
        boolean completed = hftLatch.await(120, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        CompletableFuture.allOf(hftFutures.toArray(new CompletableFuture[0])).get();

        // Then - Verify HFT results
        int totalOrders = symbols.length * ordersPerSymbol;
        double successRate = (double) successfulOrders.get() / totalOrders * 100;

        log.info("HFT Scenario Results:");
        log.info("- Total orders attempted: {}", totalOrders);
        log.info("- Successful orders: {}", successfulOrders.get());
        log.info("- Failed orders: {}", failedOrders.get());
        log.info("- Success rate: {:.2f}%", successRate);

        assertThat(successfulOrders.get()).isGreaterThan(totalOrders / 2); // At least 50% success
        assertThat(successRate).isGreaterThan(50.0);

        // Verify all symbols were processed
        verify(atLeast(symbols.length * ordersPerSymbol / 2), postRequestedFor(urlEqualTo("/api/v1/orders")));
    }

    @Test
    @DisplayName("Portfolio Management Scenario")
    void shouldHandlePortfolioManagementScenario() throws Exception {
        // Given - Setup for portfolio management
        setupAuthenticationMocks();
        setupMarketDataMocks();
        setupOrderManagementMocks();

        // Authenticate first
        LoginUserResponse loginResponse = authenticateUser();
        LoginUserControlResponse controlResponse = verifyWithSMS(loginResponse.getSessionId());

        assertThat(controlResponse.isSuccess()).isTrue();

        // When - Execute portfolio management scenario
        String[] portfolioSymbols = {"AKBNK_PORTFOLIO", "THYAO_PORTFOLIO", "GARAN_PORTFOLIO", "ISCTR_PORTFOLIO"};
        List<String> activeOrders = new ArrayList<>();

        log.info("=== Portfolio Management Scenario Started ===");

        // STEP 1: Initial Portfolio Positions
        for (String symbol : portfolioSymbols) {
            MarketDataResponse marketData = algoLabService.getMarketData(
                MarketDataRequest.builder().symbol(symbol).build()
            ).get(10, TimeUnit.SECONDS);

            assertThat(marketData.isSuccess()).isTrue();

            // Create buy orders for initial positions
            PlaceOrderRequest buyOrder = PlaceOrderRequest.builder()
                .symbol(symbol)
                .side("BUY")
                .orderType("LIMIT")
                .quantity(1000L)
                .price(marketData.getLastPrice().subtract(new BigDecimal("0.10"))) // Below market
                .timeInForce("GTC")
                .build();

            PlaceOrderResponse buyResponse = algoLabService.placeOrder(buyOrder).get(10, TimeUnit.SECONDS);
            if (buyResponse.isSuccess()) {
                activeOrders.add(buyResponse.getOrderId());
                log.info("Portfolio buy order placed: {} for {}", buyResponse.getOrderId(), symbol);
            }
        }

        // STEP 2: Monitor and manage positions
        Thread.sleep(1000); // Simulate time passage

        List<OrderStatusResponse> statusUpdates = new ArrayList<>();
        for (String orderId : activeOrders) {
            OrderStatusRequest statusRequest = OrderStatusRequest.builder()
                .orderId(orderId)
                .symbol(portfolioSymbols[statusUpdates.size() % portfolioSymbols.length])
                .build();

            OrderStatusResponse status = algoLabService.getOrderStatus(statusRequest).get(10, TimeUnit.SECONDS);
            statusUpdates.add(status);
            log.info("Order {} status: {}", orderId, status.getStatus());
        }

        // STEP 3: Rebalancing scenario
        for (int i = 0; i < Math.min(2, activeOrders.size()); i++) {
            String orderId = activeOrders.get(i);
            String symbol = portfolioSymbols[i];

            // Update order for rebalancing
            UpdateOrderRequest updateRequest = UpdateOrderRequest.builder()
                .orderId(orderId)
                .symbol(symbol)
                .quantity(1500L) // Increase position
                .price(statusUpdates.get(i).getPrice().add(new BigDecimal("0.05")))
                .build();

            UpdateOrderResponse updateResponse = algoLabService.updateOrder(updateRequest).get(10, TimeUnit.SECONDS);
            if (updateResponse.isSuccess()) {
                log.info("Portfolio rebalancing update successful for {}", symbol);
            }
        }

        // STEP 4: Risk management - Cancel some orders
        for (int i = 0; i < Math.min(1, activeOrders.size()); i++) {
            String orderId = activeOrders.get(i);
            String symbol = portfolioSymbols[i];

            CancelOrderRequest cancelRequest = CancelOrderRequest.builder()
                .orderId(orderId)
                .symbol(symbol)
                .build();

            CancelOrderResponse cancelResponse = algoLabService.cancelOrder(cancelRequest).get(10, TimeUnit.SECONDS);
            log.info("Risk management cancellation for {}: {}", symbol,
                cancelResponse.isSuccess() ? "Success" : cancelResponse.getMessage());
        }

        // Then - Verify portfolio management
        assertThat(activeOrders.size()).isEqualTo(portfolioSymbols.length);
        assertThat(statusUpdates.size()).isEqualTo(activeOrders.size());

        long successfulStatusChecks = statusUpdates.stream()
            .mapToLong(status -> status.isSuccess() ? 1 : 0)
            .sum();

        assertThat(successfulStatusChecks).isGreaterThan(portfolioSymbols.length / 2);

        log.info("=== Portfolio Management Scenario Completed ===");
        log.info("- Portfolio symbols: {}", portfolioSymbols.length);
        log.info("- Active orders created: {}", activeOrders.size());
        log.info("- Successful status checks: {}", successfulStatusChecks);
    }

    // Helper methods for test setup
    private void setupAuthenticationMocks() throws Exception {
        // Login mock
        LoginUserResponse loginResponse = LoginUserResponse.builder()
            .sessionId("e2e-session-" + System.currentTimeMillis())
            .isSuccess(true)
            .message("SMS gönderildi")
            .build();

        stubFor(post(urlEqualTo("/api/v1/login"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(loginResponse))));

        // Control mock
        LoginUserControlResponse controlResponse = LoginUserControlResponse.builder()
            .sessionId(loginResponse.getSessionId())
            .token("eyJhbGciOiJIUzI1NiJ9.e2e.token")
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
    }

    private void setupMarketDataMocks() throws Exception {
        // Generic market data mock
        stubFor(get(urlMatching("/api/v1/market-data.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(createSampleMarketData()))));
    }

    private void setupOrderManagementMocks() throws Exception {
        // Order placement mock
        stubFor(post(urlEqualTo("/api/v1/orders"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(createSampleOrderResponse()))));

        // Order status mock
        stubFor(get(urlMatching("/api/v1/orders/status.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(createSampleOrderStatus()))));

        // Order update mock
        stubFor(put(urlEqualTo("/api/v1/orders/update"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(createSampleUpdateResponse()))));

        // Order cancellation mock
        stubFor(delete(urlEqualTo("/api/v1/orders/cancel"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(createSampleCancelResponse()))));
    }

    private void setupHighFrequencyOrderMocks() throws Exception {
        // High-frequency order responses with some failures for realism
        stubFor(post(urlEqualTo("/api/v1/orders"))
            .inScenario("hft-orders")
            .whenScenarioStateIs("Started")
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(createSampleOrderResponse()))
                .withFixedDelay(50))
            .willSetStateTo("order-1"));

        stubFor(post(urlEqualTo("/api/v1/orders"))
            .inScenario("hft-orders")
            .whenScenarioStateIs("order-1")
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody("{\"isSuccess\": false, \"message\": \"Rate limited\"}"))
            .willSetStateTo("order-2"));

        stubFor(post(urlEqualTo("/api/v1/orders"))
            .inScenario("hft-orders")
            .whenScenarioStateIs("order-2")
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(createSampleOrderResponse()))
                .withFixedDelay(30))
            .willSetStateTo("Started"));
    }

    private LoginUserResponse authenticateUser() throws Exception {
        LoginUserRequest loginRequest = LoginUserRequest.builder()
            .username("e2e_test_user")
            .password("test_password")
            .build();

        return algoLabService.loginUser(loginRequest).get(10, TimeUnit.SECONDS);
    }

    private LoginUserControlResponse verifyWithSMS(String sessionId) throws Exception {
        LoginUserControlRequest controlRequest = LoginUserControlRequest.builder()
            .sessionId(sessionId)
            .smsCode("123456")
            .build();

        return algoLabService.loginUserControl(controlRequest).get(10, TimeUnit.SECONDS);
    }

    private MarketDataResponse createSampleMarketData() {
        return MarketDataResponse.builder()
            .symbol("TEST_SYMBOL")
            .lastPrice(new BigDecimal("45.50"))
            .changePercent(new BigDecimal("2.35"))
            .bidPrice(new BigDecimal("45.45"))
            .askPrice(new BigDecimal("45.55"))
            .volume(1000000L)
            .isSuccess(true)
            .timestamp(System.currentTimeMillis())
            .build();
    }

    private PlaceOrderResponse createSampleOrderResponse() {
        return PlaceOrderResponse.builder()
            .orderId("E2E_ORDER_" + System.currentTimeMillis())
            .symbol("TEST_SYMBOL")
            .side("BUY")
            .orderType("LIMIT")
            .quantity(1000L)
            .price(new BigDecimal("45.50"))
            .status("NEW")
            .isSuccess(true)
            .message("Emir başarıyla iletildi")
            .transactionTime(System.currentTimeMillis())
            .build();
    }

    private OrderStatusResponse createSampleOrderStatus() {
        return OrderStatusResponse.builder()
            .orderId("E2E_ORDER_" + System.currentTimeMillis())
            .symbol("TEST_SYMBOL")
            .side("BUY")
            .orderType("LIMIT")
            .originalQuantity(1000L)
            .executedQuantity(300L)
            .remainingQuantity(700L)
            .price(new BigDecimal("45.50"))
            .avgPrice(new BigDecimal("45.48"))
            .status("PARTIALLY_FILLED")
            .timeInForce("GTC")
            .orderTime(System.currentTimeMillis() - 60000)
            .updateTime(System.currentTimeMillis())
            .isSuccess(true)
            .build();
    }

    private UpdateOrderResponse createSampleUpdateResponse() {
        return UpdateOrderResponse.builder()
            .orderId("E2E_ORDER_" + System.currentTimeMillis())
            .symbol("TEST_SYMBOL")
            .newQuantity(1500L)
            .newPrice(new BigDecimal("46.00"))
            .status("REPLACED")
            .isSuccess(true)
            .message("Emir güncellendi")
            .transactionTime(System.currentTimeMillis())
            .build();
    }

    private CancelOrderResponse createSampleCancelResponse() {
        return CancelOrderResponse.builder()
            .orderId("E2E_ORDER_" + System.currentTimeMillis())
            .symbol("TEST_SYMBOL")
            .originalQuantity(1000L)
            .executedQuantity(300L)
            .status("CANCELED")
            .isSuccess(true)
            .message("Emir iptal edildi")
            .transactionTime(System.currentTimeMillis())
            .build();
    }
}