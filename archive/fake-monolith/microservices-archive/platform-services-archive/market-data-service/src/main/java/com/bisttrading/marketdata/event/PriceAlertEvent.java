package com.bisttrading.marketdata.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class PriceAlertEvent extends MarketDataEvent {
    private String symbol;
    private BigDecimal currentPrice;
    private BigDecimal alertPrice;
    private String alertType; // "ABOVE", "BELOW", "EQUAL"
}