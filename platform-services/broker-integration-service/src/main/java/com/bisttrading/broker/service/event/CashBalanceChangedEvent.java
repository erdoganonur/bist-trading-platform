package com.bisttrading.broker.service.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class CashBalanceChangedEvent extends PortfolioEvent {
    private BigDecimal previousBalance;
    private BigDecimal newBalance;
    private BigDecimal change;
}