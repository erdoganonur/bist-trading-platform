package com.bisttrading.broker.service.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class MarginUsageChangedEvent extends PortfolioEvent {
    private BigDecimal previousMargin;
    private BigDecimal newMargin;
    private BigDecimal change;
}