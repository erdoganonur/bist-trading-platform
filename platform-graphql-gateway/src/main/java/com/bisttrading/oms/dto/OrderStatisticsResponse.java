package com.bisttrading.oms.dto;

import java.math.BigDecimal;

public class OrderStatisticsResponse {
    private long totalOrders;
    private long filledOrders;
    private BigDecimal totalVolume;
    private BigDecimal filledVolume;

    public long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(long totalOrders) { this.totalOrders = totalOrders; }
    public long getFilledOrders() { return filledOrders; }
    public void setFilledOrders(long filledOrders) { this.filledOrders = filledOrders; }
    public BigDecimal getTotalVolume() { return totalVolume; }
    public void setTotalVolume(BigDecimal totalVolume) { this.totalVolume = totalVolume; }
    public BigDecimal getFilledVolume() { return filledVolume; }
    public void setFilledVolume(BigDecimal filledVolume) { this.filledVolume = filledVolume; }
}