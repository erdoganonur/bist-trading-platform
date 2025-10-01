package com.bisttrading.core.domain.events;

import com.bisttrading.core.domain.valueobjects.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain event representing the opening of a new trading position.
 * Published when a position is opened as a result of order execution.
 */
@Getter
public class PositionOpenedEvent extends BaseDomainEvent {

    private final String positionId;
    private final String userId;
    private final String portfolioId;
    private final Symbol symbol;
    private final PositionSide positionSide;
    private final Quantity quantity;
    private final Price averagePrice;
    private final Money totalValue;
    private final Money commission;
    private final LocalDateTime openedAt;
    private final String triggerOrderId;
    private final String triggerExecutionId;

    public PositionOpenedEvent(String positionId, String userId, String portfolioId, Symbol symbol,
                             PositionSide positionSide, Quantity quantity, Price averagePrice,
                             Money totalValue, Money commission, LocalDateTime openedAt,
                             String triggerOrderId, String triggerExecutionId) {
        super(positionId);
        this.positionId = positionId;
        this.userId = userId;
        this.portfolioId = portfolioId;
        this.symbol = symbol;
        this.positionSide = positionSide;
        this.quantity = quantity;
        this.averagePrice = averagePrice;
        this.totalValue = totalValue;
        this.commission = commission;
        this.openedAt = openedAt;
        this.triggerOrderId = triggerOrderId;
        this.triggerExecutionId = triggerExecutionId;
    }

    /**
     * Reconstructs event from stored data.
     */
    public PositionOpenedEvent(UUID eventId, LocalDateTime occurredOn, String positionId, String userId,
                             String portfolioId, Symbol symbol, PositionSide positionSide, Quantity quantity,
                             Price averagePrice, Money totalValue, Money commission, LocalDateTime openedAt,
                             String triggerOrderId, String triggerExecutionId) {
        super(eventId, occurredOn, positionId);
        this.positionId = positionId;
        this.userId = userId;
        this.portfolioId = portfolioId;
        this.symbol = symbol;
        this.positionSide = positionSide;
        this.quantity = quantity;
        this.averagePrice = averagePrice;
        this.totalValue = totalValue;
        this.commission = commission;
        this.openedAt = openedAt;
        this.triggerOrderId = triggerOrderId;
        this.triggerExecutionId = triggerExecutionId;
    }

    /**
     * Position side enumeration.
     */
    public enum PositionSide {
        LONG("LONG", "Uzun Pozisyon"),
        SHORT("SHORT", "Kısa Pozisyon");

        private final String code;
        private final String description;

        PositionSide(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() { return code; }
        public String getDescription() { return description; }

        public boolean isLong() { return this == LONG; }
        public boolean isShort() { return this == SHORT; }
    }

    /**
     * Gets the net value after commission.
     *
     * @return Net position value
     */
    public Money getNetValue() {
        return totalValue.add(commission);
    }

    /**
     * Calculates the commission rate as percentage of total value.
     *
     * @return Commission rate as percentage
     */
    public double getCommissionRate() {
        if (totalValue.isZero()) {
            return 0.0;
        }
        return commission.getAmount().doubleValue() / totalValue.getAmount().doubleValue() * 100.0;
    }

    /**
     * Checks if this is a long position.
     *
     * @return true if long position
     */
    public boolean isLongPosition() {
        return positionSide.isLong();
    }

    /**
     * Checks if this is a short position.
     *
     * @return true if short position
     */
    public boolean isShortPosition() {
        return positionSide.isShort();
    }

    /**
     * Gets the position value for given market price.
     *
     * @param marketPrice Current market price
     * @return Current position value
     */
    public Money getCurrentValue(Price marketPrice) {
        return Money.of(marketPrice.getValue().multiply(quantity.getValue()), totalValue.getCurrency());
    }

    /**
     * Calculates unrealized P&L for given market price.
     *
     * @param marketPrice Current market price
     * @return Unrealized P&L
     */
    public Money getUnrealizedPnL(Price marketPrice) {
        Money currentValue = getCurrentValue(marketPrice);
        if (positionSide.isLong()) {
            return currentValue.subtract(totalValue);
        } else {
            return totalValue.subtract(currentValue);
        }
    }

    @Override
    public String toString() {
        return String.format("PositionOpenedEvent{positionId='%s', userId='%s', symbol=%s, " +
                           "side=%s, quantity=%s, avgPrice=%s, value=%s}",
            positionId, userId, symbol.getCode(), positionSide, quantity, averagePrice, totalValue);
    }
}