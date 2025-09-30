package com.bisttrading.broker.service.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class PortfolioInitializedEvent extends PortfolioEvent {
    private String accountId;
    private BigDecimal totalEquity;
    private BigDecimal cashBalance;
    private BigDecimal portfolioValue;
    private Integer positionCount;
}