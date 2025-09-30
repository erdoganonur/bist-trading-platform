package com.bisttrading.marketdata.event;

import com.bisttrading.marketdata.model.MarketData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class MarketDataUpdateEvent extends MarketDataEvent {
    private String symbol;
    private MarketData marketData;
}