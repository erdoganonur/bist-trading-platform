package com.bisttrading.broker.websocket.event;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@Builder
public class PositionUpdateEvent {
    private String userId;
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal averagePrice;
    private BigDecimal currentPrice;
    private BigDecimal unrealizedPnl;
    private LocalDateTime timestamp;
}