package com.bisttrading.broker.testutil;

import com.bisttrading.broker.dto.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * AlgoLab test data builder utility for creating realistic test DTOs
 * Provides fluent API for building AlgoLab integration test objects
 */
public class AlgoLabTestDataBuilder {

    private static final Random random = new Random();

    // Common test symbols
    public static final String[] SYMBOLS = {"AKBNK", "THYAO", "GARAN", "ISCTR", "ASELS", "TUPRS", "SISE", "PETKM"};

    // =============================================
    // AUTHENTICATION DTOs
    // =============================================

    public static LoginUserRequestBuilder loginUserRequest() {
        return new LoginUserRequestBuilder();
    }

    @Builder
    @Data
    public static class LoginUserRequestBuilder {
        private String username;
        private String password;

        public LoginUserRequestBuilder withDefaults() {
            this.username = "test_user_" + random.nextInt(1000);
            this.password = "test_password_" + random.nextInt(1000);
            return this;
        }

        public LoginUserRequestBuilder username(String username) {
            this.username = username;
            return this;
        }

        public LoginUserRequestBuilder password(String password) {
            this.password = password;
            return this;
        }

        public LoginUserRequest build() {
            return LoginUserRequest.builder()
                .username(username)
                .password(password)
                .build();
        }
    }

    public static LoginUserResponseBuilder loginUserResponse() {
        return new LoginUserResponseBuilder();
    }

    @Builder
    @Data
    public static class LoginUserResponseBuilder {
        private String sessionId;
        private Boolean isSuccess;
        private String message;
        private String errorCode;

        public LoginUserResponseBuilder withDefaults() {
            this.sessionId = "SESSION_" + System.currentTimeMillis();
            this.isSuccess = true;
            this.message = "SMS gönderildi";
            return this;
        }

        public LoginUserResponseBuilder successful() {
            this.isSuccess = true;
            return this;
        }

        public LoginUserResponseBuilder failed(String errorCode, String message) {
            this.isSuccess = false;
            this.errorCode = errorCode;
            this.message = message;
            return this;
        }

        public LoginUserResponseBuilder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public LoginUserResponse build() {
            return LoginUserResponse.builder()
                .sessionId(sessionId)
                .isSuccess(isSuccess)
                .message(message)
                .errorCode(errorCode)
                .build();
        }
    }

    public static LoginUserControlResponseBuilder loginUserControlResponse() {
        return new LoginUserControlResponseBuilder();
    }

    @Builder
    @Data
    public static class LoginUserControlResponseBuilder {
        private String sessionId;
        private String token;
        private String tokenType;
        private Boolean isSuccess;
        private String message;
        private String errorCode;
        private Long expiresIn;

        public LoginUserControlResponseBuilder withDefaults() {
            this.sessionId = "SESSION_" + System.currentTimeMillis();
            this.token = "eyJhbGciOiJIUzI1NiJ9." + System.currentTimeMillis() + ".token";
            this.tokenType = "Bearer";
            this.isSuccess = true;
            this.message = "Giriş başarılı";
            this.expiresIn = 3600L;
            return this;
        }

        public LoginUserControlResponseBuilder successful() {
            this.isSuccess = true;
            return this;
        }

        public LoginUserControlResponseBuilder failed(String errorCode, String message) {
            this.isSuccess = false;
            this.errorCode = errorCode;
            this.message = message;
            return this;
        }

        public LoginUserControlResponseBuilder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public LoginUserControlResponse build() {
            return LoginUserControlResponse.builder()
                .sessionId(sessionId)
                .token(token)
                .tokenType(tokenType)
                .isSuccess(isSuccess)
                .message(message)
                .errorCode(errorCode)
                .expiresIn(expiresIn)
                .build();
        }
    }

    // =============================================
    // ORDER MANAGEMENT DTOs
    // =============================================

    public static PlaceOrderRequestBuilder placeOrderRequest() {
        return new PlaceOrderRequestBuilder();
    }

    @Builder
    @Data
    public static class PlaceOrderRequestBuilder {
        private String symbol;
        private String side;
        private String orderType;
        private Long quantity;
        private BigDecimal price;
        private BigDecimal stopPrice;
        private String timeInForce;

        public PlaceOrderRequestBuilder withDefaults() {
            this.symbol = randomSymbol();
            this.side = randomSide();
            this.orderType = "LIMIT";
            this.quantity = randomLong(100, 5000);
            this.price = randomPrice(50, 1000);
            this.timeInForce = "GTC";
            return this;
        }

        public PlaceOrderRequestBuilder buyOrder(String symbol, Long quantity, BigDecimal price) {
            this.symbol = symbol;
            this.side = "BUY";
            this.orderType = "LIMIT";
            this.quantity = quantity;
            this.price = price;
            return this;
        }

        public PlaceOrderRequestBuilder sellOrder(String symbol, Long quantity, BigDecimal price) {
            this.symbol = symbol;
            this.side = "SELL";
            this.orderType = "LIMIT";
            this.quantity = quantity;
            this.price = price;
            return this;
        }

        public PlaceOrderRequestBuilder marketOrder(String symbol, String side, Long quantity) {
            this.symbol = symbol;
            this.side = side;
            this.orderType = "MARKET";
            this.quantity = quantity;
            this.price = null;
            return this;
        }

        public PlaceOrderRequestBuilder stopLossOrder(String symbol, Long quantity, BigDecimal stopPrice) {
            this.symbol = symbol;
            this.side = "SELL";
            this.orderType = "STOP_LOSS";
            this.quantity = quantity;
            this.stopPrice = stopPrice;
            return this;
        }

        public PlaceOrderRequest build() {
            return PlaceOrderRequest.builder()
                .symbol(symbol)
                .side(side)
                .orderType(orderType)
                .quantity(quantity)
                .price(price)
                .stopPrice(stopPrice)
                .timeInForce(timeInForce)
                .build();
        }
    }

    public static PlaceOrderResponseBuilder placeOrderResponse() {
        return new PlaceOrderResponseBuilder();
    }

    @Builder
    @Data
    public static class PlaceOrderResponseBuilder {
        private String orderId;
        private String symbol;
        private String orderType;
        private String side;
        private Long quantity;
        private BigDecimal price;
        private BigDecimal stopPrice;
        private String status;
        private Long filledQuantity;
        private BigDecimal avgPrice;
        private Boolean isSuccess;
        private String message;
        private String errorCode;
        private Long transactionTime;

        public PlaceOrderResponseBuilder withDefaults() {
            this.orderId = "ORDER_" + System.currentTimeMillis();
            this.symbol = randomSymbol();
            this.orderType = "LIMIT";
            this.side = randomSide();
            this.quantity = randomLong(100, 5000);
            this.price = randomPrice(50, 1000);
            this.status = "NEW";
            this.filledQuantity = 0L;
            this.isSuccess = true;
            this.message = "Emir başarıyla iletildi";
            this.transactionTime = System.currentTimeMillis();
            return this;
        }

        public PlaceOrderResponseBuilder successful() {
            this.isSuccess = true;
            return this;
        }

        public PlaceOrderResponseBuilder failed(String errorCode, String message) {
            this.isSuccess = false;
            this.errorCode = errorCode;
            this.message = message;
            return this;
        }

        public PlaceOrderResponseBuilder orderId(String orderId) {
            this.orderId = orderId;
            return this;
        }

        public PlaceOrderResponseBuilder status(String status) {
            this.status = status;
            return this;
        }

        public PlaceOrderResponseBuilder filled(Long filledQuantity, BigDecimal avgPrice) {
            this.filledQuantity = filledQuantity;
            this.avgPrice = avgPrice;
            this.status = filledQuantity.equals(quantity) ? "FILLED" : "PARTIALLY_FILLED";
            return this;
        }

        public PlaceOrderResponse build() {
            return PlaceOrderResponse.builder()
                .orderId(orderId)
                .symbol(symbol)
                .orderType(orderType)
                .side(side)
                .quantity(quantity)
                .price(price)
                .stopPrice(stopPrice)
                .status(status)
                .filledQuantity(filledQuantity)
                .avgPrice(avgPrice)
                .isSuccess(isSuccess)
                .message(message)
                .errorCode(errorCode)
                .transactionTime(transactionTime)
                .build();
        }
    }

    public static OrderStatusResponseBuilder orderStatusResponse() {
        return new OrderStatusResponseBuilder();
    }

    @Builder
    @Data
    public static class OrderStatusResponseBuilder {
        private String orderId;
        private String symbol;
        private String side;
        private String orderType;
        private Long originalQuantity;
        private Long executedQuantity;
        private Long remainingQuantity;
        private BigDecimal price;
        private BigDecimal avgPrice;
        private String status;
        private String timeInForce;
        private Long orderTime;
        private Long updateTime;
        private Boolean isSuccess;
        private String message;
        private String errorCode;

        public OrderStatusResponseBuilder withDefaults() {
            this.orderId = "ORDER_" + System.currentTimeMillis();
            this.symbol = randomSymbol();
            this.side = randomSide();
            this.orderType = "LIMIT";
            this.originalQuantity = randomLong(100, 5000);
            this.executedQuantity = randomLong(0, originalQuantity);
            this.remainingQuantity = originalQuantity - executedQuantity;
            this.price = randomPrice(50, 1000);
            this.avgPrice = price.subtract(new BigDecimal(String.valueOf(random.nextDouble() * 0.1)));
            this.status = executedQuantity == 0 ? "NEW" : (executedQuantity.equals(originalQuantity) ? "FILLED" : "PARTIALLY_FILLED");
            this.timeInForce = "GTC";
            this.orderTime = System.currentTimeMillis() - 60000;
            this.updateTime = System.currentTimeMillis();
            this.isSuccess = true;
            return this;
        }

        public OrderStatusResponseBuilder orderId(String orderId) {
            this.orderId = orderId;
            return this;
        }

        public OrderStatusResponseBuilder status(String status) {
            this.status = status;
            return this;
        }

        public OrderStatusResponseBuilder partiallyFilled(Long executedQuantity) {
            this.executedQuantity = executedQuantity;
            this.remainingQuantity = originalQuantity - executedQuantity;
            this.status = "PARTIALLY_FILLED";
            return this;
        }

        public OrderStatusResponseBuilder filled() {
            this.executedQuantity = originalQuantity;
            this.remainingQuantity = 0L;
            this.status = "FILLED";
            return this;
        }

        public OrderStatusResponseBuilder canceled() {
            this.status = "CANCELED";
            return this;
        }

        public OrderStatusResponse build() {
            return OrderStatusResponse.builder()
                .orderId(orderId)
                .symbol(symbol)
                .side(side)
                .orderType(orderType)
                .originalQuantity(originalQuantity)
                .executedQuantity(executedQuantity)
                .remainingQuantity(remainingQuantity)
                .price(price)
                .avgPrice(avgPrice)
                .status(status)
                .timeInForce(timeInForce)
                .orderTime(orderTime)
                .updateTime(updateTime)
                .isSuccess(isSuccess)
                .message(message)
                .errorCode(errorCode)
                .build();
        }
    }

    // =============================================
    // MARKET DATA DTOs
    // =============================================

    public static MarketDataResponseBuilder marketDataResponse() {
        return new MarketDataResponseBuilder();
    }

    @Builder
    @Data
    public static class MarketDataResponseBuilder {
        private String symbol;
        private BigDecimal lastPrice;
        private BigDecimal changePercent;
        private BigDecimal changeAmount;
        private BigDecimal bidPrice;
        private BigDecimal askPrice;
        private Long bidSize;
        private Long askSize;
        private Long volume;
        private BigDecimal value;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal open;
        private BigDecimal close;
        private BigDecimal previousClose;
        private Long tradesCount;
        private Long timestamp;
        private Boolean isMarketOpen;
        private String marketStatus;
        private LocalDateTime nextMarketOpen;
        private Boolean isSuccess;
        private String message;
        private String errorCode;

        public MarketDataResponseBuilder withDefaults() {
            this.symbol = randomSymbol();
            this.lastPrice = randomPrice(50, 1000);
            this.changePercent = new BigDecimal(String.valueOf((random.nextDouble() - 0.5) * 10)); // -5% to +5%
            this.changeAmount = lastPrice.multiply(changePercent).divide(new BigDecimal("100"));
            this.bidPrice = lastPrice.subtract(new BigDecimal("0.05"));
            this.askPrice = lastPrice.add(new BigDecimal("0.05"));
            this.bidSize = randomLong(100, 2000);
            this.askSize = randomLong(100, 2000);
            this.volume = randomLong(100000, 10000000);
            this.value = lastPrice.multiply(new BigDecimal(volume));
            this.high = lastPrice.add(new BigDecimal(String.valueOf(random.nextDouble() * 5)));
            this.low = lastPrice.subtract(new BigDecimal(String.valueOf(random.nextDouble() * 5)));
            this.open = new BigDecimal(String.valueOf(low.doubleValue() + random.nextDouble() * (high.doubleValue() - low.doubleValue())));
            this.close = lastPrice;
            this.previousClose = lastPrice.subtract(changeAmount);
            this.tradesCount = randomLong(1000, 50000);
            this.timestamp = System.currentTimeMillis();
            this.isMarketOpen = true;
            this.marketStatus = "OPEN";
            this.isSuccess = true;
            return this;
        }

        public MarketDataResponseBuilder symbol(String symbol) {
            this.symbol = symbol;
            return this;
        }

        public MarketDataResponseBuilder lastPrice(BigDecimal lastPrice) {
            this.lastPrice = lastPrice;
            return this;
        }

        public MarketDataResponseBuilder marketClosed() {
            this.isMarketOpen = false;
            this.marketStatus = "CLOSED";
            this.nextMarketOpen = LocalDateTime.now().plusHours(12);
            return this;
        }

        public MarketDataResponseBuilder failed(String errorCode, String message) {
            this.isSuccess = false;
            this.errorCode = errorCode;
            this.message = message;
            return this;
        }

        public MarketDataResponse build() {
            return MarketDataResponse.builder()
                .symbol(symbol)
                .lastPrice(lastPrice)
                .changePercent(changePercent)
                .changeAmount(changeAmount)
                .bidPrice(bidPrice)
                .askPrice(askPrice)
                .bidSize(bidSize)
                .askSize(askSize)
                .volume(volume)
                .value(value)
                .high(high)
                .low(low)
                .open(open)
                .close(close)
                .previousClose(previousClose)
                .tradesCount(tradesCount)
                .timestamp(timestamp)
                .isMarketOpen(isMarketOpen)
                .marketStatus(marketStatus)
                .nextMarketOpen(nextMarketOpen)
                .isSuccess(isSuccess)
                .message(message)
                .errorCode(errorCode)
                .build();
        }
    }

    // =============================================
    // SCENARIO BUILDERS
    // =============================================

    public static TradingSessionScenarioBuilder tradingSessionScenario() {
        return new TradingSessionScenarioBuilder();
    }

    @Builder
    @Data
    public static class TradingSessionScenarioBuilder {
        private String username;
        private String password;
        private String[] symbols;
        private int orderCount;
        private BigDecimal basePrice;
        private double priceVariation;

        public TradingSessionScenarioBuilder withDefaults() {
            this.username = "scenario_user_" + random.nextInt(1000);
            this.password = "scenario_password";
            this.symbols = new String[]{"AKBNK", "THYAO", "GARAN"};
            this.orderCount = 5;
            this.basePrice = new BigDecimal("100.00");
            this.priceVariation = 0.1; // 10% price variation
            return this;
        }

        public TradingSessionScenarioBuilder symbols(String... symbols) {
            this.symbols = symbols;
            return this;
        }

        public TradingSessionScenarioBuilder orderCount(int orderCount) {
            this.orderCount = orderCount;
            return this;
        }

        public List<PlaceOrderRequest> buildOrderSequence() {
            List<PlaceOrderRequest> orders = new ArrayList<>();

            for (int i = 0; i < orderCount; i++) {
                String symbol = symbols[i % symbols.length];
                String side = i % 2 == 0 ? "BUY" : "SELL";
                BigDecimal price = basePrice.add(new BigDecimal(String.valueOf((random.nextDouble() - 0.5) * basePrice.doubleValue() * priceVariation)));
                Long quantity = randomLong(100, 2000);

                PlaceOrderRequest order = placeOrderRequest()
                    .symbol(symbol)
                    .side(side)
                    .orderType("LIMIT")
                    .quantity(quantity)
                    .price(price)
                    .timeInForce("GTC")
                    .build();

                orders.add(order);
            }

            return orders;
        }

        public List<MarketDataRequest> buildMarketDataRequests() {
            List<MarketDataRequest> requests = new ArrayList<>();

            for (String symbol : symbols) {
                MarketDataRequest request = MarketDataRequest.builder()
                    .symbol(symbol)
                    .build();
                requests.add(request);
            }

            return requests;
        }
    }

    // =============================================
    // UTILITY METHODS
    // =============================================

    public static String randomSymbol() {
        return SYMBOLS[random.nextInt(SYMBOLS.length)];
    }

    public static String randomSide() {
        return random.nextBoolean() ? "BUY" : "SELL";
    }

    public static BigDecimal randomPrice(double min, double max) {
        double price = min + (max - min) * random.nextDouble();
        return new BigDecimal(String.format("%.2f", price));
    }

    public static Long randomLong(long min, long max) {
        return min + (long) (random.nextDouble() * (max - min));
    }

    // =============================================
    // ERROR SCENARIO BUILDERS
    // =============================================

    public static class ErrorScenarios {

        public static LoginUserResponse invalidCredentials() {
            return loginUserResponse()
                .failed("INVALID_CREDENTIALS", "Kullanıcı adı veya şifre hatalı")
                .build();
        }

        public static LoginUserControlResponse smsTimeout() {
            return loginUserControlResponse()
                .failed("SMS_TIMEOUT", "SMS doğrulama süresi doldu")
                .build();
        }

        public static PlaceOrderResponse insufficientBalance() {
            return placeOrderResponse()
                .failed("INSUFFICIENT_BALANCE", "Yetersiz bakiye")
                .build();
        }

        public static PlaceOrderResponse marketClosed() {
            return placeOrderResponse()
                .failed("MARKET_CLOSED", "Piyasa kapalı")
                .build();
        }

        public static PlaceOrderResponse invalidParameters() {
            return placeOrderResponse()
                .failed("INVALID_PARAMETERS", "Geçersiz emir parametreleri")
                .build();
        }

        public static OrderStatusResponse orderNotFound() {
            return orderStatusResponse()
                .isSuccess(false)
                .errorCode("ORDER_NOT_FOUND")
                .message("Emir bulunamadı")
                .build();
        }

        public static MarketDataResponse invalidSymbol() {
            return marketDataResponse()
                .failed("INVALID_SYMBOL", "Geçersiz sembol")
                .build();
        }

        public static MarketDataResponse rateLimited() {
            return marketDataResponse()
                .failed("RATE_LIMITED", "Çok fazla istek")
                .build();
        }
    }

    // =============================================
    // MOCK RESPONSE SEQUENCES
    // =============================================

    public static class ResponseSequences {

        public static List<OrderStatusResponse> orderLifecycle(String orderId, String symbol) {
            List<OrderStatusResponse> sequence = new ArrayList<>();

            // NEW
            sequence.add(orderStatusResponse()
                .orderId(orderId)
                .symbol(symbol)
                .status("NEW")
                .executedQuantity(0L)
                .build());

            // PARTIALLY_FILLED
            sequence.add(orderStatusResponse()
                .orderId(orderId)
                .symbol(symbol)
                .partiallyFilled(300L)
                .build());

            // FILLED
            sequence.add(orderStatusResponse()
                .orderId(orderId)
                .symbol(symbol)
                .filled()
                .build());

            return sequence;
        }

        public static List<MarketDataResponse> marketDataStream(String symbol, int count) {
            List<MarketDataResponse> stream = new ArrayList<>();
            BigDecimal currentPrice = randomPrice(50, 200);

            for (int i = 0; i < count; i++) {
                // Simulate price movement
                double change = (random.nextGaussian() * 0.01); // 1% volatility
                currentPrice = currentPrice.add(new BigDecimal(String.valueOf(currentPrice.doubleValue() * change)));

                if (currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    currentPrice = new BigDecimal("1.00");
                }

                MarketDataResponse response = marketDataResponse()
                    .withDefaults()
                    .symbol(symbol)
                    .lastPrice(currentPrice)
                    .timestamp(System.currentTimeMillis() + (i * 1000))
                    .build();

                stream.add(response);
            }

            return stream;
        }
    }
}