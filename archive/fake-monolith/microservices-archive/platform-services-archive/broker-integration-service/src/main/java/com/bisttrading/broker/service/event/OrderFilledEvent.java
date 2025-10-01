package com.bisttrading.broker.service.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class OrderFilledEvent extends OrderEvent {
    private Integer quantity;
    private BigDecimal averagePrice;
    private BigDecimal totalValue;
    private BigDecimal commission;
}