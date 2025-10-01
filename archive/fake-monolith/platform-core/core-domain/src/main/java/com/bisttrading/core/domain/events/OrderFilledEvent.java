package com.bisttrading.core.domain.events;

import com.bisttrading.core.domain.valueobjects.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain event representing the filling (execution) of a trading order.
 * Published when an order is executed either fully or partially.
 */
@Getter
public class OrderFilledEvent extends BaseDomainEvent {

    private final String orderId;
    private final String userId;
    private final String executionId;
    private final Symbol symbol;
    private final OrderCreatedEvent.OrderSide orderSide;
    private final Quantity filledQuantity;
    private final Quantity remainingQuantity;
    private final Price executionPrice;
    private final Money executionValue;
    private final Money commission;
    private final LocalDateTime executionTime;
    private final String counterpartyId;
    private final boolean isFullyFilled;
    private final String portfolioId;

    public OrderFilledEvent(String orderId, String userId, String executionId, Symbol symbol,
                          OrderCreatedEvent.OrderSide orderSide, Quantity filledQuantity,
                          Quantity remainingQuantity, Price executionPrice, Money executionValue,
                          Money commission, LocalDateTime executionTime, String counterpartyId,
                          boolean isFullyFilled, String portfolioId) {
        super(orderId);
        this.orderId = orderId;
        this.userId = userId;
        this.executionId = executionId;
        this.symbol = symbol;
        this.orderSide = orderSide;
        this.filledQuantity = filledQuantity;
        this.remainingQuantity = remainingQuantity;
        this.executionPrice = executionPrice;
        this.executionValue = executionValue;
        this.commission = commission;
        this.executionTime = executionTime;
        this.counterpartyId = counterpartyId;
        this.isFullyFilled = isFullyFilled;
        this.portfolioId = portfolioId;
    }

    /**
     * Reconstructs event from stored data.
     */
    public OrderFilledEvent(UUID eventId, LocalDateTime occurredOn, String orderId, String userId,
                          String executionId, Symbol symbol, OrderCreatedEvent.OrderSide orderSide,
                          Quantity filledQuantity, Quantity remainingQuantity, Price executionPrice,
                          Money executionValue, Money commission, LocalDateTime executionTime,
                          String counterpartyId, boolean isFullyFilled, String portfolioId) {
        super(eventId, occurredOn, orderId);
        this.orderId = orderId;
        this.userId = userId;
        this.executionId = executionId;
        this.symbol = symbol;
        this.orderSide = orderSide;
        this.filledQuantity = filledQuantity;
        this.remainingQuantity = remainingQuantity;
        this.executionPrice = executionPrice;
        this.executionValue = executionValue;
        this.commission = commission;
        this.executionTime = executionTime;
        this.counterpartyId = counterpartyId;
        this.isFullyFilled = isFullyFilled;
        this.portfolioId = portfolioId;
    }

    /**
     * Gets the fill percentage of the order.
     *
     * @return Fill percentage as double (0.0 to 1.0)
     */
    public double getFillPercentage() {
        Quantity totalQuantity = filledQuantity.add(remainingQuantity);
        if (totalQuantity.isZero()) {
            return 0.0;
        }
        return filledQuantity.percentageOf(totalQuantity).doubleValue() / 100.0;
    }

    /**
     * Checks if this is a partial fill.
     *
     * @return true if partially filled
     */
    public boolean isPartialFill() {
        return !isFullyFilled && filledQuantity.isPositive();
    }

    /**
     * Gets the net value after commission.
     *
     * @return Net execution value
     */
    public Money getNetValue() {
        if (orderSide == OrderCreatedEvent.OrderSide.BUY) {
            return executionValue.add(commission);
        } else {
            return executionValue.subtract(commission);
        }
    }

    @Override
    public String toString() {
        return String.format("OrderFilledEvent{orderId='%s', executionId='%s', symbol=%s, side=%s, " +
                           "filled=%s, price=%s, value=%s, fullyFilled=%s}",
            orderId, executionId, symbol.getCode(), orderSide, filledQuantity,
            executionPrice, executionValue, isFullyFilled);
    }
}