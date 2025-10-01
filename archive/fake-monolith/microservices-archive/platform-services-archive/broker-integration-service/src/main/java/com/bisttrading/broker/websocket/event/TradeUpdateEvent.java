package com.bisttrading.broker.websocket.event;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@Builder
public class TradeUpdateEvent {
    private String tradeId;
    private String symbol;
    private BigDecimal price;
    private BigDecimal quantity;
    private String side;
    private LocalDateTime timestamp;
}