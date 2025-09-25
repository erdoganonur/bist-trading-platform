package com.bisttrading.broker.websocket.event;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@Builder
public class PortfolioUpdateEvent {
    private String userId;
    private BigDecimal totalValue;
    private BigDecimal availableCash;
    private BigDecimal totalPnl;
    private BigDecimal unrealizedPnl;
    private LocalDateTime timestamp;
}