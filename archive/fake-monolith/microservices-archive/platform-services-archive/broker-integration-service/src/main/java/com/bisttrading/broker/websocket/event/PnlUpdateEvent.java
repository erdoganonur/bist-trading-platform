package com.bisttrading.broker.websocket.event;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@Builder
public class PnlUpdateEvent {
    private String userId;
    private String symbol;
    private BigDecimal realizedPnl;
    private BigDecimal unrealizedPnl;
    private BigDecimal totalPnl;
    private LocalDateTime timestamp;
}