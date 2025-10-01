package com.bisttrading.broker.service.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class PositionQuantityChangedEvent extends PortfolioEvent {
    private String symbol;
    private Integer previousQuantity;
    private Integer newQuantity;
    private Integer quantityChange;
}