package com.bisttrading.broker.websocket.event;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@Builder
public class CashBalanceUpdateEvent {
    private String userId;
    private BigDecimal balance;
    private BigDecimal availableBalance;
    private BigDecimal blockedBalance;
    private String currency;
    private LocalDateTime timestamp;
}