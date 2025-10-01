package com.bisttrading.oms.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderStatistics {
    private long totalOrders;
    private long activeOrders;
    private long filledOrders;
    private long cancelledOrders;
    private long rejectedOrders;

    private BigDecimal totalVolume;
    private BigDecimal totalValue;
    private BigDecimal totalCommission;

    private BigDecimal averageOrderSize;
    private BigDecimal fillRate;

    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private String userId;
}