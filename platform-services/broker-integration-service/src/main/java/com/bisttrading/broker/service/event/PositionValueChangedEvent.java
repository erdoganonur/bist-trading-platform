package com.bisttrading.broker.service.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class PositionValueChangedEvent extends PortfolioEvent {
    private String symbol;
    private BigDecimal previousValue;
    private BigDecimal newValue;
    private BigDecimal valueChange;
}