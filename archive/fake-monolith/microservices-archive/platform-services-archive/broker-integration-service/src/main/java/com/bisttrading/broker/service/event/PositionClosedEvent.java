package com.bisttrading.broker.service.event;

import com.bisttrading.broker.algolab.model.PositionSide;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class PositionClosedEvent extends PortfolioEvent {
    private String symbol;
    private PositionSide side;
    private Integer quantity;
    private BigDecimal averageCost;
    private BigDecimal realizedPnl;
}