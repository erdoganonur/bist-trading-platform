package com.bisttrading.broker.websocket.event;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@Builder
public class OrderFillUpdateEvent {
    private String orderId;
    private String userId;
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal filledQuantity;
    private BigDecimal price;
    private LocalDateTime timestamp;
    private String side;
    private String fillType;
}