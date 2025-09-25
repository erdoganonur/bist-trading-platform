package com.bisttrading.broker.service.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class UnrealizedPnlChangedEvent extends PortfolioEvent {
    private BigDecimal previousPnl;
    private BigDecimal newPnl;
    private BigDecimal change;
}