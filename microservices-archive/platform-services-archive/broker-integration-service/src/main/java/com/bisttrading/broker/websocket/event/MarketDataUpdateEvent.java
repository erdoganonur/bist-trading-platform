package com.bisttrading.broker.websocket.event;

import com.bisttrading.broker.algolab.model.MarketData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class MarketDataUpdateEvent extends WebSocketEvent {
    private String symbol;
    private MarketData marketData;
}