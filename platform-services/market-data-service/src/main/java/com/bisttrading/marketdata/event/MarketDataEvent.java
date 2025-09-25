package com.bisttrading.marketdata.event;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Data
@SuperBuilder
public abstract class MarketDataEvent {
    private Instant timestamp;
}