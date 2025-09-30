package com.bisttrading.graphql.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Feign client for Broker Integration Service
 *
 * Provides async access to broker integration operations for GraphQL gateway
 */
@FeignClient(
    name = "broker-integration-service",
    url = "${service-clients.broker-integration.base-url:http://localhost:8084}",
    configuration = ServiceClientConfiguration.class
)
public interface BrokerIntegrationServiceClient {

    // Basic DTOs for compilation
    class PortfolioResponse {
        private String userId;
        private java.math.BigDecimal totalValue;
        private java.math.BigDecimal totalCost;
        private java.math.BigDecimal totalPnl;
        private java.math.BigDecimal totalPnlPercent;
        private java.math.BigDecimal cashBalance;
        private java.math.BigDecimal availableCash;
        private List<PositionResponse> positions;
        private java.time.OffsetDateTime lastUpdateTime;

        public static PortfolioResponse empty() {
            PortfolioResponse response = new PortfolioResponse();
            response.totalValue = java.math.BigDecimal.ZERO;
            response.totalCost = java.math.BigDecimal.ZERO;
            response.totalPnl = java.math.BigDecimal.ZERO;
            response.totalPnlPercent = java.math.BigDecimal.ZERO;
            response.cashBalance = java.math.BigDecimal.ZERO;
            response.availableCash = java.math.BigDecimal.ZERO;
            response.positions = List.of();
            response.lastUpdateTime = java.time.OffsetDateTime.now();
            return response;
        }

        // Getters and setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public java.math.BigDecimal getTotalValue() { return totalValue; }
        public void setTotalValue(java.math.BigDecimal totalValue) { this.totalValue = totalValue; }
        public java.math.BigDecimal getTotalCost() { return totalCost; }
        public void setTotalCost(java.math.BigDecimal totalCost) { this.totalCost = totalCost; }
        public java.math.BigDecimal getTotalPnl() { return totalPnl; }
        public void setTotalPnl(java.math.BigDecimal totalPnl) { this.totalPnl = totalPnl; }
        public java.math.BigDecimal getTotalPnlPercent() { return totalPnlPercent; }
        public void setTotalPnlPercent(java.math.BigDecimal totalPnlPercent) { this.totalPnlPercent = totalPnlPercent; }
        public java.math.BigDecimal getCashBalance() { return cashBalance; }
        public void setCashBalance(java.math.BigDecimal cashBalance) { this.cashBalance = cashBalance; }
        public java.math.BigDecimal getAvailableCash() { return availableCash; }
        public void setAvailableCash(java.math.BigDecimal availableCash) { this.availableCash = availableCash; }
        public List<PositionResponse> getPositions() { return positions; }
        public void setPositions(List<PositionResponse> positions) { this.positions = positions; }
        public java.time.OffsetDateTime getLastUpdateTime() { return lastUpdateTime; }
        public void setLastUpdateTime(java.time.OffsetDateTime lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }
    }

    class PositionResponse {
        private String symbol;
        private String name;
        private java.math.BigDecimal quantity;
        private java.math.BigDecimal averagePrice;
        private java.math.BigDecimal currentPrice;
        private java.math.BigDecimal marketValue;
        private java.math.BigDecimal cost;
        private java.math.BigDecimal unrealizedPnL;
        private java.math.BigDecimal unrealizedPnLPercent;
        private java.math.BigDecimal realizedPnL;
        private java.math.BigDecimal dayPnL;
        private java.math.BigDecimal dayPnLPercent;
        private java.math.BigDecimal weight;
        private java.time.OffsetDateTime firstBuyDate;
        private java.time.OffsetDateTime lastTransactionDate;

        // Getters and setters
        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public java.math.BigDecimal getQuantity() { return quantity; }
        public void setQuantity(java.math.BigDecimal quantity) { this.quantity = quantity; }
        public java.math.BigDecimal getAveragePrice() { return averagePrice; }
        public void setAveragePrice(java.math.BigDecimal averagePrice) { this.averagePrice = averagePrice; }
        public java.math.BigDecimal getCurrentPrice() { return currentPrice; }
        public void setCurrentPrice(java.math.BigDecimal currentPrice) { this.currentPrice = currentPrice; }
        public java.math.BigDecimal getMarketValue() { return marketValue; }
        public void setMarketValue(java.math.BigDecimal marketValue) { this.marketValue = marketValue; }
        public java.math.BigDecimal getCost() { return cost; }
        public void setCost(java.math.BigDecimal cost) { this.cost = cost; }
        public java.math.BigDecimal getUnrealizedPnL() { return unrealizedPnL; }
        public void setUnrealizedPnL(java.math.BigDecimal unrealizedPnL) { this.unrealizedPnL = unrealizedPnL; }
        public java.math.BigDecimal getUnrealizedPnLPercent() { return unrealizedPnLPercent; }
        public void setUnrealizedPnLPercent(java.math.BigDecimal unrealizedPnLPercent) { this.unrealizedPnLPercent = unrealizedPnLPercent; }
        public java.math.BigDecimal getRealizedPnL() { return realizedPnL; }
        public void setRealizedPnL(java.math.BigDecimal realizedPnL) { this.realizedPnL = realizedPnL; }
        public java.math.BigDecimal getDayPnL() { return dayPnL; }
        public void setDayPnL(java.math.BigDecimal dayPnL) { this.dayPnL = dayPnL; }
        public java.math.BigDecimal getDayPnLPercent() { return dayPnLPercent; }
        public void setDayPnLPercent(java.math.BigDecimal dayPnLPercent) { this.dayPnLPercent = dayPnLPercent; }
        public java.math.BigDecimal getWeight() { return weight; }
        public void setWeight(java.math.BigDecimal weight) { this.weight = weight; }
        public java.time.OffsetDateTime getFirstBuyDate() { return firstBuyDate; }
        public void setFirstBuyDate(java.time.OffsetDateTime firstBuyDate) { this.firstBuyDate = firstBuyDate; }
        public java.time.OffsetDateTime getLastTransactionDate() { return lastTransactionDate; }
        public void setLastTransactionDate(java.time.OffsetDateTime lastTransactionDate) { this.lastTransactionDate = lastTransactionDate; }
    }

    class PortfolioPerformanceResponse {
        private java.math.BigDecimal totalReturn;
        private java.math.BigDecimal totalReturnPercent;
        private java.math.BigDecimal dayReturn;
        private java.math.BigDecimal dayReturnPercent;
        private java.math.BigDecimal weekReturn;
        private java.math.BigDecimal monthReturn;
        private java.math.BigDecimal quarterReturn;
        private java.math.BigDecimal yearReturn;
        private java.math.BigDecimal winRate;
        private java.math.BigDecimal profitFactor;
        private java.math.BigDecimal sharpeRatio;
        private java.math.BigDecimal maxDrawdown;
        private java.math.BigDecimal maxDrawdownPercent;
        private java.math.BigDecimal volatility;

        public static PortfolioPerformanceResponse empty() {
            PortfolioPerformanceResponse response = new PortfolioPerformanceResponse();
            response.totalReturn = java.math.BigDecimal.ZERO;
            response.totalReturnPercent = java.math.BigDecimal.ZERO;
            response.dayReturn = java.math.BigDecimal.ZERO;
            response.dayReturnPercent = java.math.BigDecimal.ZERO;
            response.weekReturn = java.math.BigDecimal.ZERO;
            response.monthReturn = java.math.BigDecimal.ZERO;
            response.quarterReturn = java.math.BigDecimal.ZERO;
            response.yearReturn = java.math.BigDecimal.ZERO;
            response.winRate = java.math.BigDecimal.ZERO;
            response.profitFactor = java.math.BigDecimal.ZERO;
            return response;
        }

        // Getters and setters
        public java.math.BigDecimal getTotalReturn() { return totalReturn; }
        public void setTotalReturn(java.math.BigDecimal totalReturn) { this.totalReturn = totalReturn; }
        public java.math.BigDecimal getTotalReturnPercent() { return totalReturnPercent; }
        public void setTotalReturnPercent(java.math.BigDecimal totalReturnPercent) { this.totalReturnPercent = totalReturnPercent; }
        public java.math.BigDecimal getDayReturn() { return dayReturn; }
        public void setDayReturn(java.math.BigDecimal dayReturn) { this.dayReturn = dayReturn; }
        public java.math.BigDecimal getDayReturnPercent() { return dayReturnPercent; }
        public void setDayReturnPercent(java.math.BigDecimal dayReturnPercent) { this.dayReturnPercent = dayReturnPercent; }
        public java.math.BigDecimal getWeekReturn() { return weekReturn; }
        public void setWeekReturn(java.math.BigDecimal weekReturn) { this.weekReturn = weekReturn; }
        public java.math.BigDecimal getMonthReturn() { return monthReturn; }
        public void setMonthReturn(java.math.BigDecimal monthReturn) { this.monthReturn = monthReturn; }
        public java.math.BigDecimal getQuarterReturn() { return quarterReturn; }
        public void setQuarterReturn(java.math.BigDecimal quarterReturn) { this.quarterReturn = quarterReturn; }
        public java.math.BigDecimal getYearReturn() { return yearReturn; }
        public void setYearReturn(java.math.BigDecimal yearReturn) { this.yearReturn = yearReturn; }
        public java.math.BigDecimal getWinRate() { return winRate; }
        public void setWinRate(java.math.BigDecimal winRate) { this.winRate = winRate; }
        public java.math.BigDecimal getProfitFactor() { return profitFactor; }
        public void setProfitFactor(java.math.BigDecimal profitFactor) { this.profitFactor = profitFactor; }
        public java.math.BigDecimal getSharpeRatio() { return sharpeRatio; }
        public void setSharpeRatio(java.math.BigDecimal sharpeRatio) { this.sharpeRatio = sharpeRatio; }
        public java.math.BigDecimal getMaxDrawdown() { return maxDrawdown; }
        public void setMaxDrawdown(java.math.BigDecimal maxDrawdown) { this.maxDrawdown = maxDrawdown; }
        public java.math.BigDecimal getMaxDrawdownPercent() { return maxDrawdownPercent; }
        public void setMaxDrawdownPercent(java.math.BigDecimal maxDrawdownPercent) { this.maxDrawdownPercent = maxDrawdownPercent; }
        public java.math.BigDecimal getVolatility() { return volatility; }
        public void setVolatility(java.math.BigDecimal volatility) { this.volatility = volatility; }
    }

    class BrokerStatusResponse {
        private boolean connected;
        private boolean authenticated;
        private java.time.OffsetDateTime lastCheckTime;
        private java.time.OffsetDateTime sessionExpiresAt;
        private String brokerName;
        private List<String> capabilities;
        private Integer connectionLatency;
        private String lastError;
        private boolean maintenanceMode;
        private boolean tradingEnabled;

        // Getters and setters
        public boolean isConnected() { return connected; }
        public void setConnected(boolean connected) { this.connected = connected; }
        public boolean isAuthenticated() { return authenticated; }
        public void setAuthenticated(boolean authenticated) { this.authenticated = authenticated; }
        public java.time.OffsetDateTime getLastCheckTime() { return lastCheckTime; }
        public void setLastCheckTime(java.time.OffsetDateTime lastCheckTime) { this.lastCheckTime = lastCheckTime; }
        public java.time.OffsetDateTime getSessionExpiresAt() { return sessionExpiresAt; }
        public void setSessionExpiresAt(java.time.OffsetDateTime sessionExpiresAt) { this.sessionExpiresAt = sessionExpiresAt; }
        public String getBrokerName() { return brokerName; }
        public void setBrokerName(String brokerName) { this.brokerName = brokerName; }
        public List<String> getCapabilities() { return capabilities; }
        public void setCapabilities(List<String> capabilities) { this.capabilities = capabilities; }
        public Integer getConnectionLatency() { return connectionLatency; }
        public void setConnectionLatency(Integer connectionLatency) { this.connectionLatency = connectionLatency; }
        public String getLastError() { return lastError; }
        public void setLastError(String lastError) { this.lastError = lastError; }
        public boolean isMaintenanceMode() { return maintenanceMode; }
        public void setMaintenanceMode(boolean maintenanceMode) { this.maintenanceMode = maintenanceMode; }
        public boolean isTradingEnabled() { return tradingEnabled; }
        public void setTradingEnabled(boolean tradingEnabled) { this.tradingEnabled = tradingEnabled; }
    }

    // REST API Methods
    @GetMapping("/api/v1/broker/portfolio/{userId}")
    CompletableFuture<PortfolioResponse> getPortfolio(@PathVariable String userId);

    @GetMapping("/api/v1/broker/positions/{userId}")
    CompletableFuture<List<PositionResponse>> getPositions(@PathVariable String userId);

    @GetMapping("/api/v1/broker/performance/{userId}")
    CompletableFuture<PortfolioPerformanceResponse> getPortfolioPerformance(@PathVariable String userId);

    @GetMapping("/api/v1/broker/status")
    CompletableFuture<BrokerStatusResponse> getBrokerStatus();

    @GetMapping("/api/v1/broker/status/{userId}")
    CompletableFuture<BrokerStatusResponse> getUserBrokerStatus(@PathVariable String userId);

    @PostMapping("/api/v1/broker/connect/{userId}")
    CompletableFuture<BrokerStatusResponse> connectBroker(@PathVariable String userId);

    @PostMapping("/api/v1/broker/disconnect/{userId}")
    CompletableFuture<Boolean> disconnectBroker(@PathVariable String userId);

    @GetMapping("/api/v1/broker/transactions/{userId}")
    CompletableFuture<List<Object>> getTransactions(
        @PathVariable String userId,
        @RequestParam(required = false) Object filter
    );

    @GetMapping("/api/v1/broker/account/{userId}")
    CompletableFuture<Object> getAccountInfo(@PathVariable String userId);
}