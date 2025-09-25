package com.bisttrading.broker.service.event;

import com.bisttrading.broker.algolab.model.OrderStatus;
import com.bisttrading.broker.algolab.model.OrderType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class OrderModifiedEvent extends OrderEvent {
    private OrderType type;
    private Integer quantity;
    private BigDecimal price;
    private OrderStatus status;
}