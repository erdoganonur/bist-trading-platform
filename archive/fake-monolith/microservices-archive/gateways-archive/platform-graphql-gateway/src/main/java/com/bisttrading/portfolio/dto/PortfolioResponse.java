package com.bisttrading.portfolio.dto;

import java.math.BigDecimal;
import java.util.List;

public class PortfolioResponse {
    private String userId;
    private BigDecimal totalValue;
    private BigDecimal cash;
    private List<PositionDto> positions;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
    public BigDecimal getCash() { return cash; }
    public void setCash(BigDecimal cash) { this.cash = cash; }
    public List<PositionDto> getPositions() { return positions; }
    public void setPositions(List<PositionDto> positions) { this.positions = positions; }
}

class PositionDto {
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal averagePrice;
    private BigDecimal marketValue;
    
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getAveragePrice() { return averagePrice; }
    public void setAveragePrice(BigDecimal averagePrice) { this.averagePrice = averagePrice; }
    public BigDecimal getMarketValue() { return marketValue; }
    public void setMarketValue(BigDecimal marketValue) { this.marketValue = marketValue; }
}