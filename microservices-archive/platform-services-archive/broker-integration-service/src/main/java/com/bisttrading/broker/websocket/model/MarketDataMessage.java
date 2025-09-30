package com.bisttrading.broker.websocket.model;

import com.bisttrading.broker.algolab.model.MarketData;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MarketDataMessage extends WebSocketMessage {

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("data")
    private MarketData data;
}