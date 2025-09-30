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
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * AlgoLab market data entegrasyon testleri
 * Market data alma, fiyat sorgulama ve piyasa bilgisi alma işlemlerini test eder
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-integration-test.properties")
@DisplayName("AlgoLab Market Data Integration Tests")
class AlgoLabMarketDataIntegrationTest {

    @Autowired
    private AlgoLabService algoLabService;

    @Autowired
    private AlgoLabProperties algoLabProperties;

    @Autowired
    private ObjectMapper objectMapper;

    private WireMockServer wireMockServer;
    private static final int WIREMOCK_PORT = 8091;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("algolab.base-url", () -> "http://localhost:" + WIREMOCK_PORT);
        registry.add("algolab.market-data-url", () -> "http://localhost:" + WIREMOCK_PORT + "/api/v1/market-data");
        registry.add("algolab.symbols-url", () -> "http://localhost:" + WIREMOCK_PORT + "/api/v1/symbols");
        registry.add("algolab.depth-url", () -> "http://localhost:" + WIREMOCK_PORT + "/api/v1/depth");
        registry.add("algolab.trades-url", () -> "http://localhost:" + WIREMOCK_PORT + "/api/v1/trades");
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
    @DisplayName("Sembol listesi alma")
    void shouldGetSymbolsListSuccessfully() throws Exception {
        // Given - Mock symbols response
        SymbolsResponse symbolsResponse = SymbolsResponse.builder()
            .symbols(Arrays.asList(
                createSymbolInfo("AKBNK", "Akbank T.A.Ş.", "TRY", new BigDecimal("0.01")),
                createSymbolInfo("THYAO", "Türk Hava Yolları A.O.", "TRY", new BigDecimal("0.01")),
                createSymbolInfo("ISCTR", "Türkiye İş Bankası A.Ş.", "TRY", new BigDecimal("0.01")),
                createSymbolInfo("GARAN", "Türkiye Garanti Bankası A.Ş.", "TRY", new BigDecimal("0.01"))
            ))
            .totalCount(4)
            .isSuccess(true)
            .message("Sembol listesi başarıyla alındı")
            .build();

        stubFor(get(urlEqualTo("/api/v1/symbols"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(symbolsResponse))));

        // When - Get symbols list
        CompletableFuture<SymbolsResponse> future = algoLabService.getSymbols();
        SymbolsResponse response = future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getTotalCount()).isEqualTo(4);
        assertThat(response.getSymbols()).hasSize(4);
        assertThat(response.getSymbols().get(0).getSymbol()).isEqualTo("AKBNK");
        assertThat(response.getSymbols().get(1).getSymbol()).isEqualTo("THYAO");
    }

    @Test
    @DisplayName("Tek sembol market data alma")
    void shouldGetSingleSymbolMarketDataSuccessfully() throws Exception {
        // Given - Mock single symbol market data
        MarketDataResponse marketDataResponse = MarketDataResponse.builder()
            .symbol("AKBNK")
            .lastPrice(new BigDecimal("45.50"))
            .changePercent(new BigDecimal("2.35"))
            .changeAmount(new BigDecimal("1.05"))
            .bidPrice(new BigDecimal("45.45"))
            .askPrice(new BigDecimal("45.55"))
            .bidSize(1000L)
            .askSize(1500L)
            .volume(25678900L)
            .value(new BigDecimal("1168591550.00"))
            .high(new BigDecimal("46.20"))
            .low(new BigDecimal("44.80"))
            .open(new BigDecimal("44.45"))
            .close(new BigDecimal("45.50"))
            .previousClose(new BigDecimal("44.45"))
            .tradesCount(12547L)
            .timestamp(System.currentTimeMillis())
            .isSuccess(true)
            .build();

        stubFor(get(urlEqualTo("/api/v1/market-data?symbol=AKBNK"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(marketDataResponse))));

        // When - Get market data for AKBNK
        MarketDataRequest request = MarketDataRequest.builder()
            .symbol("AKBNK")
            .build();

        CompletableFuture<MarketDataResponse> future = algoLabService.getMarketData(request);
        MarketDataResponse response = future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getSymbol()).isEqualTo("AKBNK");
        assertThat(response.getLastPrice()).isEqualByComparingTo(new BigDecimal("45.50"));
        assertThat(response.getChangePercent()).isEqualByComparingTo(new BigDecimal("2.35"));
        assertThat(response.getBidPrice()).isEqualByComparingTo(new BigDecimal("45.45"));
        assertThat(response.getAskPrice()).isEqualByComparingTo(new BigDecimal("45.55"));
        assertThat(response.getVolume()).isEqualTo(25678900L);
        assertThat(response.getTradesCount()).isEqualTo(12547L);
    }

    @Test
    @DisplayName("Çoklu sembol market data alma")
    void shouldGetMultipleSymbolsMarketDataSuccessfully() throws Exception {
        // Given - Mock multiple symbols market data
        List<String> symbols = Arrays.asList("AKBNK", "THYAO", "GARAN");

        MultipleMarketDataResponse multiResponse = MultipleMarketDataResponse.builder()
            .marketData(Arrays.asList(
                createMarketData("AKBNK", new BigDecimal("45.50"), new BigDecimal("2.35")),
                createMarketData("THYAO", new BigDecimal("312.25"), new BigDecimal("-1.75")),
                createMarketData("GARAN", new BigDecimal("98.75"), new BigDecimal("0.50"))
            ))
            .totalSymbols(3)
            .isSuccess(true)
            .message("Çoklu market data başarıyla alındı")
            .build();

        stubFor(post(urlEqualTo("/api/v1/market-data/multiple"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(multiResponse))));

        // When - Get multiple symbols market data
        MultipleMarketDataRequest request = MultipleMarketDataRequest.builder()
            .symbols(symbols)
            .build();

        CompletableFuture<MultipleMarketDataResponse> future = algoLabService.getMultipleMarketData(request);
        MultipleMarketDataResponse response = future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getTotalSymbols()).isEqualTo(3);
        assertThat(response.getMarketData()).hasSize(3);
        assertThat(response.getMarketData().get(0).getSymbol()).isEqualTo("AKBNK");
        assertThat(response.getMarketData().get(1).getSymbol()).isEqualTo("THYAO");
        assertThat(response.getMarketData().get(2).getSymbol()).isEqualTo("GARAN");
    }

    @Test
    @DisplayName("Order book depth alma")
    void shouldGetOrderBookDepthSuccessfully() throws Exception {
        // Given - Mock order book depth response
        OrderBookDepthResponse depthResponse = OrderBookDepthResponse.builder()
            .symbol("AKBNK")
            .bids(Arrays.asList(
                createDepthLevel(new BigDecimal("45.45"), 1000L),
                createDepthLevel(new BigDecimal("45.40"), 2500L),
                createDepthLevel(new BigDecimal("45.35"), 1800L)
            ))
            .asks(Arrays.asList(
                createDepthLevel(new BigDecimal("45.55"), 1200L),
                createDepthLevel(new BigDecimal("45.60"), 3000L),
                createDepthLevel(new BigDecimal("45.65"), 2200L)
            ))
            .lastUpdateTime(System.currentTimeMillis())
            .isSuccess(true)
            .build();

        stubFor(get(urlEqualTo("/api/v1/depth?symbol=AKBNK&depth=5"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(depthResponse))));

        // When - Get order book depth
        OrderBookDepthRequest request = OrderBookDepthRequest.builder()
            .symbol("AKBNK")
            .depth(5)
            .build();

        CompletableFuture<OrderBookDepthResponse> future = algoLabService.getOrderBookDepth(request);
        OrderBookDepthResponse response = future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getSymbol()).isEqualTo("AKBNK");
        assertThat(response.getBids()).hasSize(3);
        assertThat(response.getAsks()).hasSize(3);
        assertThat(response.getBids().get(0).getPrice()).isEqualByComparingTo(new BigDecimal("45.45"));
        assertThat(response.getAsks().get(0).getPrice()).isEqualByComparingTo(new BigDecimal("45.55"));
    }

    @Test
    @DisplayName("Son işlemler listesi alma")
    void shouldGetRecentTradesSuccessfully() throws Exception {
        // Given - Mock recent trades response
        RecentTradesResponse tradesResponse = RecentTradesResponse.builder()
            .symbol("AKBNK")
            .trades(Arrays.asList(
                createTradeInfo(new BigDecimal("45.50"), 1000L, "BUY", System.currentTimeMillis()),
                createTradeInfo(new BigDecimal("45.48"), 500L, "SELL", System.currentTimeMillis() - 1000),
                createTradeInfo(new BigDecimal("45.52"), 750L, "BUY", System.currentTimeMillis() - 2000)
            ))
            .totalTrades(3)
            .isSuccess(true)
            .message("Son işlemler başarıyla alındı")
            .build();

        stubFor(get(urlEqualTo("/api/v1/trades?symbol=AKBNK&limit=10"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(tradesResponse))));

        // When - Get recent trades
        RecentTradesRequest request = RecentTradesRequest.builder()
            .symbol("AKBNK")
            .limit(10)
            .build();

        CompletableFuture<RecentTradesResponse> future = algoLabService.getRecentTrades(request);
        RecentTradesResponse response = future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getSymbol()).isEqualTo("AKBNK");
        assertThat(response.getTotalTrades()).isEqualTo(3);
        assertThat(response.getTrades()).hasSize(3);
        assertThat(response.getTrades().get(0).getPrice()).isEqualByComparingTo(new BigDecimal("45.50"));
    }

    @Test
    @DisplayName("Candle/OHLC data alma")
    void shouldGetCandleDataSuccessfully() throws Exception {
        // Given - Mock candle data response
        CandleDataResponse candleResponse = CandleDataResponse.builder()
            .symbol("AKBNK")
            .interval("1m")
            .candles(Arrays.asList(
                createCandleData(System.currentTimeMillis() - 60000,
                    new BigDecimal("45.40"), new BigDecimal("45.55"),
                    new BigDecimal("45.35"), new BigDecimal("45.50"), 150000L),
                createCandleData(System.currentTimeMillis() - 120000,
                    new BigDecimal("45.35"), new BigDecimal("45.45"),
                    new BigDecimal("45.30"), new BigDecimal("45.40"), 120000L)
            ))
            .totalCandles(2)
            .isSuccess(true)
            .build();

        stubFor(get(urlEqualTo("/api/v1/candles?symbol=AKBNK&interval=1m&limit=100"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(candleResponse))));

        // When - Get candle data
        CandleDataRequest request = CandleDataRequest.builder()
            .symbol("AKBNK")
            .interval("1m")
            .limit(100)
            .build();

        CompletableFuture<CandleDataResponse> future = algoLabService.getCandleData(request);
        CandleDataResponse response = future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getSymbol()).isEqualTo("AKBNK");
        assertThat(response.getInterval()).isEqualTo("1m");
        assertThat(response.getTotalCandles()).isEqualTo(2);
        assertThat(response.getCandles()).hasSize(2);
    }

    @Test
    @DisplayName("Market data güncelleme sıklığı testi")
    void shouldHandleFrequentMarketDataUpdates() throws Exception {
        // Given - Mock real-time like updates
        for (int i = 0; i < 10; i++) {
            BigDecimal price = new BigDecimal("45." + (50 + i));
            MarketDataResponse response = createMarketData("AKBNK", price, new BigDecimal("1.5"));

            stubFor(get(urlEqualTo("/api/v1/market-data?symbol=AKBNK"))
                .inScenario("price-updates")
                .whenScenarioStateIs(i == 0 ? "Started" : "update-" + (i-1))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBody(objectMapper.writeValueAsString(response))
                    .withFixedDelay(50))
                .willSetStateTo("update-" + i));
        }

        // When - Make frequent requests
        MarketDataRequest request = MarketDataRequest.builder()
            .symbol("AKBNK")
            .build();

        CompletableFuture<?>[] futures = new CompletableFuture[10];
        for (int i = 0; i < 10; i++) {
            futures[i] = algoLabService.getMarketData(request)
                .thenAccept(response -> {
                    assertThat(response.isSuccess()).isTrue();
                    assertThat(response.getSymbol()).isEqualTo("AKBNK");
                });
        }

        // Wait for all requests to complete
        CompletableFuture.allOf(futures).get(10, TimeUnit.SECONDS);

        // Then - All requests should have been processed
        verify(exactly(10), getRequestedFor(urlEqualTo("/api/v1/market-data?symbol=AKBNK")));
    }

    @Test
    @DisplayName("Geçersiz sembol işleme")
    void shouldHandleInvalidSymbol() throws Exception {
        // Given - Mock invalid symbol response
        MarketDataResponse errorResponse = MarketDataResponse.builder()
            .isSuccess(false)
            .errorCode("INVALID_SYMBOL")
            .message("Geçersiz sembol: INVALID")
            .build();

        stubFor(get(urlEqualTo("/api/v1/market-data?symbol=INVALID"))
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(errorResponse))));

        // When - Request invalid symbol
        MarketDataRequest request = MarketDataRequest.builder()
            .symbol("INVALID")
            .build();

        CompletableFuture<MarketDataResponse> future = algoLabService.getMarketData(request);
        MarketDataResponse response = future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("INVALID_SYMBOL");
        assertThat(response.getMessage()).contains("Geçersiz sembol");
    }

    @Test
    @DisplayName("Piyasa dış saatlerde data alma")
    void shouldHandleOutOfMarketHoursData() throws Exception {
        // Given - Mock out of market hours response
        MarketDataResponse response = MarketDataResponse.builder()
            .symbol("AKBNK")
            .lastPrice(new BigDecimal("45.50"))
            .isMarketOpen(false)
            .marketStatus("CLOSED")
            .nextMarketOpen(LocalDateTime.now().plusHours(12))
            .isSuccess(true)
            .message("Piyasa kapalı - Son kapanış verileri")
            .build();

        stubFor(get(urlEqualTo("/api/v1/market-data?symbol=AKBNK"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(response))));

        // When - Get market data when market is closed
        MarketDataRequest request = MarketDataRequest.builder()
            .symbol("AKBNK")
            .build();

        CompletableFuture<MarketDataResponse> future = algoLabService.getMarketData(request);
        MarketDataResponse marketDataResponse = future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(marketDataResponse.isSuccess()).isTrue();
        assertThat(marketDataResponse.isMarketOpen()).isFalse();
        assertThat(marketDataResponse.getMarketStatus()).isEqualTo("CLOSED");
        assertThat(marketDataResponse.getNextMarketOpen()).isNotNull();
    }

    // Helper methods for creating test objects
    private SymbolInfo createSymbolInfo(String symbol, String name, String currency, BigDecimal tickSize) {
        return SymbolInfo.builder()
            .symbol(symbol)
            .name(name)
            .currency(currency)
            .tickSize(tickSize)
            .minQuantity(1L)
            .maxQuantity(1000000L)
            .status("TRADING")
            .build();
    }

    private MarketDataResponse createMarketData(String symbol, BigDecimal lastPrice, BigDecimal changePercent) {
        return MarketDataResponse.builder()
            .symbol(symbol)
            .lastPrice(lastPrice)
            .changePercent(changePercent)
            .bidPrice(lastPrice.subtract(new BigDecimal("0.05")))
            .askPrice(lastPrice.add(new BigDecimal("0.05")))
            .bidSize(1000L)
            .askSize(1000L)
            .volume(1000000L)
            .timestamp(System.currentTimeMillis())
            .isSuccess(true)
            .build();
    }

    private DepthLevel createDepthLevel(BigDecimal price, Long quantity) {
        return DepthLevel.builder()
            .price(price)
            .quantity(quantity)
            .build();
    }

    private TradeInfo createTradeInfo(BigDecimal price, Long quantity, String side, long timestamp) {
        return TradeInfo.builder()
            .price(price)
            .quantity(quantity)
            .side(side)
            .timestamp(timestamp)
            .tradeId("TRADE_" + timestamp)
            .build();
    }

    private CandleData createCandleData(long timestamp, BigDecimal open, BigDecimal high,
                                      BigDecimal low, BigDecimal close, Long volume) {
        return CandleData.builder()
            .timestamp(timestamp)
            .open(open)
            .high(high)
            .low(low)
            .close(close)
            .volume(volume)
            .build();
    }
}