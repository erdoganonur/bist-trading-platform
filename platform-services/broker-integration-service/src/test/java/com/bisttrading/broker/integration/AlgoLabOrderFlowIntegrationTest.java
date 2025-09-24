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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AlgoLab emir işlem akışı entegrasyon testleri
 * Emir gönderme, güncelleme, iptal etme ve durumu takip etme işlemlerini test eder
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-integration-test.properties")
@DisplayName("AlgoLab Order Flow Integration Tests")
class AlgoLabOrderFlowIntegrationTest {

    @Autowired
    private AlgoLabService algoLabService;

    @Autowired
    private AlgoLabProperties algoLabProperties;

    @Autowired
    private ObjectMapper objectMapper;

    private WireMockServer wireMockServer;
    private static final int WIREMOCK_PORT = 8090;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("algolab.base-url", () -> "http://localhost:" + WIREMOCK_PORT);
        registry.add("algolab.order-url", () -> "http://localhost:" + WIREMOCK_PORT + "/api/v1/orders");
        registry.add("algolab.cancel-url", () -> "http://localhost:" + WIREMOCK_PORT + "/api/v1/orders/cancel");
        registry.add("algolab.update-url", () -> "http://localhost:" + WIREMOCK_PORT + "/api/v1/orders/update");
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
    @DisplayName("Başarılı al emri gönderimi")
    void shouldSendBuyOrderSuccessfully() throws Exception {
        // Given - Mock successful order response
        PlaceOrderResponse orderResponse = PlaceOrderResponse.builder()
            .orderId("ORDER_12345")
            .symbol("AKBNK")
            .orderType("LIMIT")
            .side("BUY")
            .quantity(1000L)
            .price(new BigDecimal("45.50"))
            .status("NEW")
            .isSuccess(true)
            .message("Emir başarıyla iletildi")
            .transactionTime(System.currentTimeMillis())
            .build();

        stubFor(post(urlEqualTo("/api/v1/orders"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(orderResponse))));

        // When - Place buy order
        PlaceOrderRequest orderRequest = PlaceOrderRequest.builder()
            .symbol("AKBNK")
            .side("BUY")
            .orderType("LIMIT")
            .quantity(1000L)
            .price(new BigDecimal("45.50"))
            .timeInForce("GTC")
            .build();

        CompletableFuture<PlaceOrderResponse> future = algoLabService.placeOrder(orderRequest);
        PlaceOrderResponse response = future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getOrderId()).isEqualTo("ORDER_12345");
        assertThat(response.getSymbol()).isEqualTo("AKBNK");
        assertThat(response.getSide()).isEqualTo("BUY");
        assertThat(response.getQuantity()).isEqualTo(1000L);
        assertThat(response.getPrice()).isEqualByComparingTo(new BigDecimal("45.50"));
        assertThat(response.getStatus()).isEqualTo("NEW");

        verify(postRequestedFor(urlEqualTo("/api/v1/orders"))
            .withRequestBody(containing("AKBNK"))
            .withRequestBody(containing("BUY"))
            .withRequestBody(containing("LIMIT"))
            .withRequestBody(containing("1000"))
            .withRequestBody(containing("45.50")));
    }

    @Test
    @DisplayName("Başarılı sat emri gönderimi")
    void shouldSendSellOrderSuccessfully() throws Exception {
        // Given - Mock successful sell order response
        PlaceOrderResponse orderResponse = PlaceOrderResponse.builder()
            .orderId("ORDER_67890")
            .symbol("THYAO")
            .orderType("MARKET")
            .side("SELL")
            .quantity(500L)
            .status("FILLED")
            .filledQuantity(500L)
            .avgPrice(new BigDecimal("312.25"))
            .isSuccess(true)
            .message("Emir gerçekleştirildi")
            .transactionTime(System.currentTimeMillis())
            .build();

        stubFor(post(urlEqualTo("/api/v1/orders"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(orderResponse))));

        // When - Place market sell order
        PlaceOrderRequest orderRequest = PlaceOrderRequest.builder()
            .symbol("THYAO")
            .side("SELL")
            .orderType("MARKET")
            .quantity(500L)
            .timeInForce("IOC")
            .build();

        CompletableFuture<PlaceOrderResponse> future = algoLabService.placeOrder(orderRequest);
        PlaceOrderResponse response = future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getOrderId()).isEqualTo("ORDER_67890");
        assertThat(response.getSymbol()).isEqualTo("THYAO");
        assertThat(response.getSide()).isEqualTo("SELL");
        assertThat(response.getStatus()).isEqualTo("FILLED");
        assertThat(response.getFilledQuantity()).isEqualTo(500L);
        assertThat(response.getAvgPrice()).isEqualByComparingTo(new BigDecimal("312.25"));
    }

    @Test
    @DisplayName("Stop-loss emri gönderimi")
    void shouldSendStopLossOrderSuccessfully() throws Exception {
        // Given - Mock stop-loss order response
        PlaceOrderResponse orderResponse = PlaceOrderResponse.builder()
            .orderId("STOP_ORDER_111")
            .symbol("ISCTR")
            .orderType("STOP_LOSS")
            .side("SELL")
            .quantity(200L)
            .stopPrice(new BigDecimal("18.00"))
            .status("NEW")
            .isSuccess(true)
            .message("Stop-loss emri oluşturuldu")
            .transactionTime(System.currentTimeMillis())
            .build();

        stubFor(post(urlEqualTo("/api/v1/orders"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(orderResponse))));

        // When - Place stop-loss order
        PlaceOrderRequest orderRequest = PlaceOrderRequest.builder()
            .symbol("ISCTR")
            .side("SELL")
            .orderType("STOP_LOSS")
            .quantity(200L)
            .stopPrice(new BigDecimal("18.00"))
            .timeInForce("GTC")
            .build();

        CompletableFuture<PlaceOrderResponse> future = algoLabService.placeOrder(orderRequest);
        PlaceOrderResponse response = future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getOrderType()).isEqualTo("STOP_LOSS");
        assertThat(response.getStopPrice()).isEqualByComparingTo(new BigDecimal("18.00"));
        assertThat(response.getStatus()).isEqualTo("NEW");
    }

    @Test
    @DisplayName("Emir iptali başarılı")
    void shouldCancelOrderSuccessfully() throws Exception {
        // Given - Mock cancel order response
        CancelOrderResponse cancelResponse = CancelOrderResponse.builder()
            .orderId("ORDER_12345")
            .symbol("AKBNK")
            .originalQuantity(1000L)
            .executedQuantity(300L)
            .status("CANCELED")
            .isSuccess(true)
            .message("Emir iptal edildi")
            .transactionTime(System.currentTimeMillis())
            .build();

        stubFor(delete(urlEqualTo("/api/v1/orders/cancel"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(cancelResponse))));

        // When - Cancel order
        CancelOrderRequest cancelRequest = CancelOrderRequest.builder()
            .orderId("ORDER_12345")
            .symbol("AKBNK")
            .build();

        CompletableFuture<CancelOrderResponse> future = algoLabService.cancelOrder(cancelRequest);
        CancelOrderResponse response = future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getOrderId()).isEqualTo("ORDER_12345");
        assertThat(response.getStatus()).isEqualTo("CANCELED");
        assertThat(response.getExecutedQuantity()).isEqualTo(300L);

        verify(deleteRequestedFor(urlEqualTo("/api/v1/orders/cancel"))
            .withRequestBody(containing("ORDER_12345"))
            .withRequestBody(containing("AKBNK")));
    }

    @Test
    @DisplayName("Emir güncelleme başarılı")
    void shouldUpdateOrderSuccessfully() throws Exception {
        // Given - Mock update order response
        UpdateOrderResponse updateResponse = UpdateOrderResponse.builder()
            .orderId("ORDER_12345")
            .symbol("AKBNK")
            .newQuantity(1500L)
            .newPrice(new BigDecimal("46.00"))
            .status("REPLACED")
            .isSuccess(true)
            .message("Emir güncellendi")
            .transactionTime(System.currentTimeMillis())
            .build();

        stubFor(put(urlEqualTo("/api/v1/orders/update"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(updateResponse))));

        // When - Update order
        UpdateOrderRequest updateRequest = UpdateOrderRequest.builder()
            .orderId("ORDER_12345")
            .symbol("AKBNK")
            .quantity(1500L)
            .price(new BigDecimal("46.00"))
            .build();

        CompletableFuture<UpdateOrderResponse> future = algoLabService.updateOrder(updateRequest);
        UpdateOrderResponse response = future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getOrderId()).isEqualTo("ORDER_12345");
        assertThat(response.getNewQuantity()).isEqualTo(1500L);
        assertThat(response.getNewPrice()).isEqualByComparingTo(new BigDecimal("46.00"));
        assertThat(response.getStatus()).isEqualTo("REPLACED");
    }

    @Test
    @DisplayName("Emir durumu sorgulama")
    void shouldQueryOrderStatusSuccessfully() throws Exception {
        // Given - Mock order status response
        OrderStatusResponse statusResponse = OrderStatusResponse.builder()
            .orderId("ORDER_12345")
            .symbol("AKBNK")
            .side("BUY")
            .orderType("LIMIT")
            .originalQuantity(1000L)
            .executedQuantity(700L)
            .remainingQuantity(300L)
            .price(new BigDecimal("45.50"))
            .avgPrice(new BigDecimal("45.48"))
            .status("PARTIALLY_FILLED")
            .timeInForce("GTC")
            .orderTime(System.currentTimeMillis() - 60000)
            .updateTime(System.currentTimeMillis())
            .isSuccess(true)
            .build();

        stubFor(get(urlEqualTo("/api/v1/orders/status?orderId=ORDER_12345&symbol=AKBNK"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(statusResponse))));

        // When - Query order status
        OrderStatusRequest statusRequest = OrderStatusRequest.builder()
            .orderId("ORDER_12345")
            .symbol("AKBNK")
            .build();

        CompletableFuture<OrderStatusResponse> future = algoLabService.getOrderStatus(statusRequest);
        OrderStatusResponse response = future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getOrderId()).isEqualTo("ORDER_12345");
        assertThat(response.getStatus()).isEqualTo("PARTIALLY_FILLED");
        assertThat(response.getExecutedQuantity()).isEqualTo(700L);
        assertThat(response.getRemainingQuantity()).isEqualTo(300L);
        assertThat(response.getAvgPrice()).isEqualByComparingTo(new BigDecimal("45.48"));
    }

    @Test
    @DisplayName("Geçersiz emir parametreleri işleme")
    void shouldHandleInvalidOrderParameters() throws Exception {
        // Given - Mock validation error response
        PlaceOrderResponse errorResponse = PlaceOrderResponse.builder()
            .isSuccess(false)
            .errorCode("INVALID_PARAMETERS")
            .message("Geçersiz emir parametreleri: Miktar sıfırdan büyük olmalıdır")
            .build();

        stubFor(post(urlEqualTo("/api/v1/orders"))
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(errorResponse))));

        // When - Send invalid order
        PlaceOrderRequest invalidRequest = PlaceOrderRequest.builder()
            .symbol("AKBNK")
            .side("BUY")
            .orderType("LIMIT")
            .quantity(0L) // Invalid quantity
            .price(new BigDecimal("45.50"))
            .build();

        CompletableFuture<PlaceOrderResponse> future = algoLabService.placeOrder(invalidRequest);
        PlaceOrderResponse response = future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("INVALID_PARAMETERS");
        assertThat(response.getMessage()).contains("Miktar sıfırdan büyük olmalıdır");
    }

    @Test
    @DisplayName("Yetersiz bakiye durumu işleme")
    void shouldHandleInsufficientBalance() throws Exception {
        // Given - Mock insufficient balance response
        PlaceOrderResponse errorResponse = PlaceOrderResponse.builder()
            .isSuccess(false)
            .errorCode("INSUFFICIENT_BALANCE")
            .message("Yetersiz bakiye: Mevcut bakiye 10000 TL, gereken miktar 50000 TL")
            .build();

        stubFor(post(urlEqualTo("/api/v1/orders"))
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(errorResponse))));

        // When - Send order with insufficient balance
        PlaceOrderRequest orderRequest = PlaceOrderRequest.builder()
            .symbol("AKBNK")
            .side("BUY")
            .orderType("LIMIT")
            .quantity(1000L)
            .price(new BigDecimal("50.00")) // Expensive order
            .build();

        CompletableFuture<PlaceOrderResponse> future = algoLabService.placeOrder(orderRequest);
        PlaceOrderResponse response = future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("INSUFFICIENT_BALANCE");
        assertThat(response.getMessage()).contains("Yetersiz bakiye");
    }

    @Test
    @DisplayName("Piyasa kapandıktan sonra emir gönderimi")
    void shouldHandleMarketClosedOrder() throws Exception {
        // Given - Mock market closed response
        PlaceOrderResponse errorResponse = PlaceOrderResponse.builder()
            .isSuccess(false)
            .errorCode("MARKET_CLOSED")
            .message("Piyasa kapalı: Emir işlemi reddedildi")
            .build();

        stubFor(post(urlEqualTo("/api/v1/orders"))
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(errorResponse))));

        // When - Send order when market is closed
        PlaceOrderRequest orderRequest = PlaceOrderRequest.builder()
            .symbol("AKBNK")
            .side("BUY")
            .orderType("LIMIT")
            .quantity(1000L)
            .price(new BigDecimal("45.50"))
            .build();

        CompletableFuture<PlaceOrderResponse> future = algoLabService.placeOrder(orderRequest);
        PlaceOrderResponse response = future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("MARKET_CLOSED");
        assertThat(response.getMessage()).contains("Piyasa kapalı");
    }

    @Test
    @DisplayName("Emir bulunamadı durumu işleme")
    void shouldHandleOrderNotFound() throws Exception {
        // Given - Mock order not found response
        CancelOrderResponse errorResponse = CancelOrderResponse.builder()
            .isSuccess(false)
            .errorCode("ORDER_NOT_FOUND")
            .message("Emir bulunamadı: ORDER_NONEXISTENT")
            .build();

        stubFor(delete(urlEqualTo("/api/v1/orders/cancel"))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(errorResponse))));

        // When - Try to cancel non-existent order
        CancelOrderRequest cancelRequest = CancelOrderRequest.builder()
            .orderId("ORDER_NONEXISTENT")
            .symbol("AKBNK")
            .build();

        CompletableFuture<CancelOrderResponse> future = algoLabService.cancelOrder(cancelRequest);
        CancelOrderResponse response = future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("ORDER_NOT_FOUND");
        assertThat(response.getMessage()).contains("Emir bulunamadı");
    }

    @Test
    @DisplayName("Emir zaten gerçekleştirilmiş durumu işleme")
    void shouldHandleOrderAlreadyFilled() throws Exception {
        // Given - Mock order already filled response
        CancelOrderResponse errorResponse = CancelOrderResponse.builder()
            .orderId("ORDER_FILLED")
            .isSuccess(false)
            .errorCode("ORDER_ALREADY_FILLED")
            .message("Emir zaten gerçekleştirilmiş, iptal edilemez")
            .build();

        stubFor(delete(urlEqualTo("/api/v1/orders/cancel"))
            .willReturn(aResponse()
                .withStatus(400)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody(objectMapper.writeValueAsString(errorResponse))));

        // When - Try to cancel filled order
        CancelOrderRequest cancelRequest = CancelOrderRequest.builder()
            .orderId("ORDER_FILLED")
            .symbol("AKBNK")
            .build();

        CompletableFuture<CancelOrderResponse> future = algoLabService.cancelOrder(cancelRequest);
        CancelOrderResponse response = future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getErrorCode()).isEqualTo("ORDER_ALREADY_FILLED");
        assertThat(response.getMessage()).contains("zaten gerçekleştirilmiş");
    }

    @Test
    @DisplayName("Batch emir işleme")
    void shouldProcessBatchOrdersSuccessfully() throws Exception {
        // Given - Mock responses for multiple orders
        for (int i = 1; i <= 5; i++) {
            PlaceOrderResponse orderResponse = PlaceOrderResponse.builder()
                .orderId("BATCH_ORDER_" + i)
                .symbol("AKBNK")
                .orderType("LIMIT")
                .side("BUY")
                .quantity(100L)
                .price(new BigDecimal("45." + (50 + i)))
                .status("NEW")
                .isSuccess(true)
                .message("Batch emir " + i + " başarılı")
                .transactionTime(System.currentTimeMillis())
                .build();

            stubFor(post(urlEqualTo("/api/v1/orders"))
                .withRequestBody(containing("45." + (50 + i)))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .withBody(objectMapper.writeValueAsString(orderResponse))
                    .withFixedDelay(50))); // Simulate processing delay
        }

        // When - Send multiple orders concurrently
        CompletableFuture<?>[] futures = new CompletableFuture[5];
        for (int i = 1; i <= 5; i++) {
            PlaceOrderRequest request = PlaceOrderRequest.builder()
                .symbol("AKBNK")
                .side("BUY")
                .orderType("LIMIT")
                .quantity(100L)
                .price(new BigDecimal("45." + (50 + i)))
                .build();

            final int orderId = i;
            futures[i-1] = algoLabService.placeOrder(request)
                .thenAccept(response -> {
                    assertThat(response.isSuccess()).isTrue();
                    assertThat(response.getOrderId()).isEqualTo("BATCH_ORDER_" + orderId);
                });
        }

        // Wait for all orders to complete
        CompletableFuture.allOf(futures).get(10, TimeUnit.SECONDS);

        // Then - All orders should have been processed
        verify(exactly(5), postRequestedFor(urlEqualTo("/api/v1/orders")));
    }
}