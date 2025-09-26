package com.bisttrading.oms.event;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderFilledEvent {
    private String orderId;
    private String userId;
    private String symbol;
    private BigDecimal filledQuantity;
    private BigDecimal averagePrice;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}