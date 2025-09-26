package com.bisttrading.graphql.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Feign client for Market Data Service
 *
 * Provides async access to market data operations for GraphQL gateway
 */
@FeignClient(
    name = "market-data-service",
    url = "${service-clients.market-data.base-url:http://localhost:8083}",
    configuration = ServiceClientConfiguration.class
)
public interface MarketDataServiceClient {

    // Basic DTOs for compilation
    class MarketDataResponse {
        private String symbol;
        private java.math.BigDecimal price;
        private java.math.BigDecimal change;
        private java.math.BigDecimal changePercent;
        private java.math.BigDecimal volume;
        private java.time.OffsetDateTime lastTradeTime;
        private java.math.BigDecimal bid;
        private java.math.BigDecimal ask;
        private java.math.BigDecimal high;
        private java.math.BigDecimal low;
        private java.math.BigDecimal open;

        public static MarketDataResponse empty(String symbol) {
            MarketDataResponse response = new MarketDataResponse();
            response.symbol = symbol;
            response.price = java.math.BigDecimal.ZERO;
            response.change = java.math.BigDecimal.ZERO;
            response.changePercent = java.math.BigDecimal.ZERO;
            response.volume = java.math.BigDecimal.ZERO;
            response.lastTradeTime = java.time.OffsetDateTime.now();
            return response;
        }

        // Getters and setters
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public java.math.BigDecimal getPrice() { return price; }
        public void setPrice(java.math.BigDecimal price) { this.price = price; }
        public java.math.BigDecimal getChange() { return change; }
        public void setChange(java.math.BigDecimal change) { this.change = change; }
        public java.math.BigDecimal getChangePercent() { return changePercent; }
        public void setChangePercent(java.math.BigDecimal changePercent) { this.changePercent = changePercent; }
        public java.math.BigDecimal getVolume() { return volume; }
        public void setVolume(java.math.BigDecimal volume) { this.volume = volume; }
        public java.time.OffsetDateTime getLastTradeTime() { return lastTradeTime; }
        public void setLastTradeTime(java.time.OffsetDateTime lastTradeTime) { this.lastTradeTime = lastTradeTime; }
        public java.math.BigDecimal getBid() { return bid; }
        public void setBid(java.math.BigDecimal bid) { this.bid = bid; }
        public java.math.BigDecimal getAsk() { return ask; }
        public void setAsk(java.math.BigDecimal ask) { this.ask = ask; }
        public java.math.BigDecimal getHigh() { return high; }
        public void setHigh(java.math.BigDecimal high) { this.high = high; }
        public java.math.BigDecimal getLow() { return low; }
        public void setLow(java.math.BigDecimal low) { this.low = low; }
        public java.math.BigDecimal getOpen() { return open; }
        public void setOpen(java.math.BigDecimal open) { this.open = open; }
    }

    class MarketSymbolResponse {
        private String symbol;
        private String name;
        private String type;
        private String exchange;
        private String currency;
        private String sector;
        private String industry;
        private boolean isActive;

        // Getters and setters
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getExchange() { return exchange; }
        public void setExchange(String exchange) { this.exchange = exchange; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getSector() { return sector; }
        public void setSector(String sector) { this.sector = sector; }
        public String getIndustry() { return industry; }
        public void setIndustry(String industry) { this.industry = industry; }
        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { isActive = active; }
    }

    class TechnicalAnalysisResponse {
        private String symbol;
        private java.math.BigDecimal rsi;
        private java.math.BigDecimal sma20;
        private java.math.BigDecimal sma50;
        private java.math.BigDecimal ema12;
        private java.math.BigDecimal ema26;

        // Getters and setters
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public java.math.BigDecimal getRsi() { return rsi; }
        public void setRsi(java.math.BigDecimal rsi) { this.rsi = rsi; }
        public java.math.BigDecimal getSma20() { return sma20; }
        public void setSma20(java.math.BigDecimal sma20) { this.sma20 = sma20; }
        public java.math.BigDecimal getSma50() { return sma50; }
        public void setSma50(java.math.BigDecimal sma50) { this.sma50 = sma50; }
        public java.math.BigDecimal getEma12() { return ema12; }
        public void setEma12(java.math.BigDecimal ema12) { this.ema12 = ema12; }
        public java.math.BigDecimal getEma26() { return ema26; }
        public void setEma26(java.math.BigDecimal ema26) { this.ema26 = ema26; }
    }

    class MarketOverviewResponse {
        private String marketStatus;
        private java.time.OffsetDateTime marketTime;
        private int totalSymbols;
        private int advancers;
        private int decliners;
        private java.math.BigDecimal totalVolume;

        // Getters and setters
        public String getMarketStatus() { return marketStatus; }
        public void setMarketStatus(String marketStatus) { this.marketStatus = marketStatus; }
        public java.time.OffsetDateTime getMarketTime() { return marketTime; }
        public void setMarketTime(java.time.OffsetDateTime marketTime) { this.marketTime = marketTime; }
        public int getTotalSymbols() { return totalSymbols; }
        public void setTotalSymbols(int totalSymbols) { this.totalSymbols = totalSymbols; }
        public int getAdvancers() { return advancers; }
        public void setAdvancers(int advancers) { this.advancers = advancers; }
        public int getDecliners() { return decliners; }
        public void setDecliners(int decliners) { this.decliners = decliners; }
        public java.math.BigDecimal getTotalVolume() { return totalVolume; }
        public void setTotalVolume(java.math.BigDecimal totalVolume) { this.totalVolume = totalVolume; }
    }

    class TechnicalIndicatorRequest {
        private String symbol;
        private List<String> indicators;
        private String period;
        private int count;

        public static TechnicalIndicatorRequestBuilder builder() {
            return new TechnicalIndicatorRequestBuilder();
        }

        // Getters and setters
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public List<String> getIndicators() { return indicators; }
        public void setIndicators(List<String> indicators) { this.indicators = indicators; }
        public String getPeriod() { return period; }
        public void setPeriod(String period) { this.period = period; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }

        public static class TechnicalIndicatorRequestBuilder {
            private TechnicalIndicatorRequest request = new TechnicalIndicatorRequest();

            public TechnicalIndicatorRequestBuilder symbol(String symbol) {
                request.setSymbol(symbol);
                return this;
            }

            public TechnicalIndicatorRequestBuilder indicators(List<String> indicators) {
                request.setIndicators(indicators);
                return this;
            }

            public TechnicalIndicatorRequestBuilder period(String period) {
                request.setPeriod(period);
                return this;
            }

            public TechnicalIndicatorRequestBuilder count(int count) {
                request.setCount(count);
                return this;
            }

            public TechnicalIndicatorRequest build() {
                return request;
            }
        }
    }

    class MarketDepthResponse {
        private String symbol;
        private List<Object> bids;
        private List<Object> asks;
        private java.time.OffsetDateTime lastUpdate;
        private java.math.BigDecimal totalBidVolume;
        private java.math.BigDecimal totalAskVolume;

        public static MarketDepthResponse empty(String symbol) {
            MarketDepthResponse response = new MarketDepthResponse();
            response.symbol = symbol;
            response.bids = List.of();
            response.asks = List.of();
            response.lastUpdate = java.time.OffsetDateTime.now();
            response.totalBidVolume = java.math.BigDecimal.ZERO;
            response.totalAskVolume = java.math.BigDecimal.ZERO;
            return response;
        }

        // Getters and setters
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public List<Object> getBids() { return bids; }
        public void setBids(List<Object> bids) { this.bids = bids; }
        public List<Object> getAsks() { return asks; }
        public void setAsks(List<Object> asks) { this.asks = asks; }
        public java.time.OffsetDateTime getLastUpdate() { return lastUpdate; }
        public void setLastUpdate(java.time.OffsetDateTime lastUpdate) { this.lastUpdate = lastUpdate; }
        public java.math.BigDecimal getTotalBidVolume() { return totalBidVolume; }
        public void setTotalBidVolume(java.math.BigDecimal totalBidVolume) { this.totalBidVolume = totalBidVolume; }
        public java.math.BigDecimal getTotalAskVolume() { return totalAskVolume; }
        public void setTotalAskVolume(java.math.BigDecimal totalAskVolume) { this.totalAskVolume = totalAskVolume; }
    }

    class TechnicalIndicatorsResponse {
        private String symbol;
        private java.math.BigDecimal rsi;
        private java.math.BigDecimal sma20;
        private java.math.BigDecimal sma50;
        private java.math.BigDecimal sma200;

        public static TechnicalIndicatorsResponse empty(String symbol) {
            TechnicalIndicatorsResponse response = new TechnicalIndicatorsResponse();
            response.symbol = symbol;
            return response;
        }

        // Getters and setters
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public java.math.BigDecimal getRsi() { return rsi; }
        public void setRsi(java.math.BigDecimal rsi) { this.rsi = rsi; }
        public java.math.BigDecimal getSma20() { return sma20; }
        public void setSma20(java.math.BigDecimal sma20) { this.sma20 = sma20; }
        public java.math.BigDecimal getSma50() { return sma50; }
        public void setSma50(java.math.BigDecimal sma50) { this.sma50 = sma50; }
        public java.math.BigDecimal getSma200() { return sma200; }
        public void setSma200(java.math.BigDecimal sma200) { this.sma200 = sma200; }
    }

    // REST API Methods
    @GetMapping("/api/v1/market-data/{symbol}")
    CompletableFuture<MarketDataResponse> getMarketData(@PathVariable String symbol);

    @PostMapping("/api/v1/market-data/batch")
    CompletableFuture<List<MarketDataResponse>> getMarketDataBatch(@RequestBody List<String> symbols);

    @GetMapping("/api/v1/market-data/search")
    CompletableFuture<List<MarketSymbolResponse>> searchSymbols(
        @RequestParam String query,
        @RequestParam(defaultValue = "10") int limit
    );

    @PostMapping("/api/v1/market-data/technical-analysis")
    CompletableFuture<TechnicalAnalysisResponse> getTechnicalAnalysis(@RequestBody TechnicalIndicatorRequest request);

    @GetMapping("/api/v1/market-data/overview")
    CompletableFuture<MarketOverviewResponse> getMarketOverview(@RequestParam(required = false) Object period);

    @GetMapping("/api/v1/market-data/status")
    CompletableFuture<String> getMarketStatus();

    @GetMapping("/api/v1/market-data/trading-hours")
    CompletableFuture<Object> getTradingHours();

    @GetMapping("/api/v1/market-data/symbols")
    CompletableFuture<List<MarketSymbolResponse>> getAvailableSymbols();

    @GetMapping("/api/v1/market-data/{symbol}/depth")
    CompletableFuture<MarketDepthResponse> getMarketDepth(@PathVariable String symbol);

    @GetMapping("/api/v1/market-data/{symbol}/indicators")
    CompletableFuture<TechnicalIndicatorsResponse> getTechnicalIndicators(@PathVariable String symbol);

    // Subscription methods (WebSocket/Server-Sent Events)
    default Flux<Object> subscribeToMarketDataUpdates(List<String> symbols) {
        // Mock implementation - real version would use WebSocket
        return Flux.empty();
    }

    default Flux<Object> subscribeToPriceAlerts(String userId) {
        // Mock implementation - real version would use WebSocket
        return Flux.empty();
    }
}