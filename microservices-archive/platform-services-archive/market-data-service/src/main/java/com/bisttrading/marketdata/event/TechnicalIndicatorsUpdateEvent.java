package com.bisttrading.marketdata.event;

import com.bisttrading.marketdata.model.TechnicalIndicators;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class TechnicalIndicatorsUpdateEvent extends MarketDataEvent {
    private String symbol;
    private TechnicalIndicators indicators;
    private String trendSignal;
    private String rsiSignal;
    private String macdSignal;
    private String overallSignal;
}