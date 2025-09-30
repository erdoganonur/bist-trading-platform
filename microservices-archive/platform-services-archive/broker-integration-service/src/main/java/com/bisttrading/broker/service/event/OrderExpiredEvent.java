package com.bisttrading.broker.service.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class OrderExpiredEvent extends OrderEvent {
    private Integer originalQuantity;
    private Integer filledQuantity;
    private Integer remainingQuantity;
}