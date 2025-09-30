package com.bisttrading.broker.service.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class OrderPartiallyFilledEvent extends OrderEvent {
    private Integer originalQuantity;
    private Integer filledQuantity;
    private Integer remainingQuantity;
    private BigDecimal averagePrice;
    private BigDecimal commission;
}