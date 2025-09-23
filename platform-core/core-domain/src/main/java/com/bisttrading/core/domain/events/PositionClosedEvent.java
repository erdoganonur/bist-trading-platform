package com.bisttrading.core.domain.events;

import com.bisttrading.core.domain.valueobjects.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain event representing the closing of a trading position.
 * Published when a position is closed either fully or partially.
 */
@Getter
public class PositionClosedEvent extends BaseDomainEvent {

    private final String positionId;
    private final String userId;
    private final String portfolioId;
    private final Symbol symbol;
    private final PositionOpenedEvent.PositionSide positionSide;
    private final Quantity closedQuantity;
    private final Quantity remainingQuantity;
    private final Price averageOpenPrice;
    private final Price averageClosePrice;
    private final Money openValue;
    private final Money closeValue;
    private final Money realizedPnL;
    private final Money commission;
    private final LocalDateTime closedAt;
    private final LocalDateTime originalOpenedAt;
    private final String triggerOrderId;
    private final String triggerExecutionId;
    private final boolean isFullyClosed;
    private final BigDecimal holdingPeriodDays;

    public PositionClosedEvent(String positionId, String userId, String portfolioId, Symbol symbol,
                             PositionOpenedEvent.PositionSide positionSide, Quantity closedQuantity,
                             Quantity remainingQuantity, Price averageOpenPrice, Price averageClosePrice,
                             Money openValue, Money closeValue, Money realizedPnL, Money commission,
                             LocalDateTime closedAt, LocalDateTime originalOpenedAt, String triggerOrderId,
                             String triggerExecutionId, boolean isFullyClosed, BigDecimal holdingPeriodDays) {
        super(positionId);
        this.positionId = positionId;
        this.userId = userId;
        this.portfolioId = portfolioId;
        this.symbol = symbol;
        this.positionSide = positionSide;
        this.closedQuantity = closedQuantity;
        this.remainingQuantity = remainingQuantity;
        this.averageOpenPrice = averageOpenPrice;
        this.averageClosePrice = averageClosePrice;
        this.openValue = openValue;
        this.closeValue = closeValue;
        this.realizedPnL = realizedPnL;
        this.commission = commission;
        this.closedAt = closedAt;
        this.originalOpenedAt = originalOpenedAt;
        this.triggerOrderId = triggerOrderId;
        this.triggerExecutionId = triggerExecutionId;
        this.isFullyClosed = isFullyClosed;
        this.holdingPeriodDays = holdingPeriodDays;
    }

    /**
     * Reconstructs event from stored data.
     */
    public PositionClosedEvent(UUID eventId, LocalDateTime occurredOn, String positionId, String userId,
                             String portfolioId, Symbol symbol, PositionOpenedEvent.PositionSide positionSide,
                             Quantity closedQuantity, Quantity remainingQuantity, Price averageOpenPrice,
                             Price averageClosePrice, Money openValue, Money closeValue, Money realizedPnL,
                             Money commission, LocalDateTime closedAt, LocalDateTime originalOpenedAt,
                             String triggerOrderId, String triggerExecutionId, boolean isFullyClosed,
                             BigDecimal holdingPeriodDays) {
        super(eventId, occurredOn, positionId);
        this.positionId = positionId;
        this.userId = userId;
        this.portfolioId = portfolioId;
        this.symbol = symbol;
        this.positionSide = positionSide;
        this.closedQuantity = closedQuantity;
        this.remainingQuantity = remainingQuantity;
        this.averageOpenPrice = averageOpenPrice;
        this.averageClosePrice = averageClosePrice;
        this.openValue = openValue;
        this.closeValue = closeValue;
        this.realizedPnL = realizedPnL;
        this.commission = commission;
        this.closedAt = closedAt;
        this.originalOpenedAt = originalOpenedAt;
        this.triggerOrderId = triggerOrderId;
        this.triggerExecutionId = triggerExecutionId;
        this.isFullyClosed = isFullyClosed;
        this.holdingPeriodDays = holdingPeriodDays;
    }

    /**
     * Checks if this is a partial closure.
     *
     * @return true if partially closed
     */
    public boolean isPartialClosure() {
        return !isFullyClosed && remainingQuantity.isPositive();
    }

    /**
     * Gets the closure percentage.
     *
     * @return Closure percentage as double (0.0 to 1.0)
     */
    public double getClosurePercentage() {
        Quantity totalQuantity = closedQuantity.add(remainingQuantity);
        if (totalQuantity.isZero()) {
            return 0.0;
        }
        return closedQuantity.percentageOf(totalQuantity).doubleValue() / 100.0;
    }

    /**
     * Calculates the return on investment (ROI) percentage.
     *
     * @return ROI percentage
     */
    public double getROIPercentage() {
        if (openValue.isZero()) {
            return 0.0;
        }
        return realizedPnL.getAmount().doubleValue() / openValue.getAmount().doubleValue() * 100.0;
    }

    /**
     * Gets the net P&L after commission.
     *
     * @return Net realized P&L
     */
    public Money getNetPnL() {
        return realizedPnL.subtract(commission);
    }

    /**
     * Checks if the position was profitable.
     *
     * @return true if profitable
     */
    public boolean isProfitable() {
        return getNetPnL().isPositive();
    }

    /**
     * Checks if this was a long position.
     *
     * @return true if long position
     */
    public boolean isLongPosition() {
        return positionSide.isLong();
    }

    /**
     * Checks if this was a short position.
     *
     * @return true if short position
     */
    public boolean isShortPosition() {
        return positionSide.isShort();
    }

    /**
     * Checks if this was a short-term holding (less than 1 year).
     *
     * @return true if short-term
     */
    public boolean isShortTerm() {
        return holdingPeriodDays.compareTo(new BigDecimal("365")) < 0;
    }

    /**
     * Checks if this was a long-term holding (1 year or more).
     *
     * @return true if long-term
     */
    public boolean isLongTerm() {
        return holdingPeriodDays.compareTo(new BigDecimal("365")) >= 0;
    }

    /**
     * Gets the price improvement/deterioration compared to open price.
     *
     * @return Price difference
     */
    public BigDecimal getPriceImprovement() {
        if (positionSide.isLong()) {
            return averageClosePrice.differenceFrom(averageOpenPrice);
        } else {
            return averageOpenPrice.differenceFrom(averageClosePrice);
        }
    }

    /**
     * Gets the price improvement percentage.
     *
     * @return Price improvement percentage
     */
    public double getPriceImprovementPercentage() {
        if (positionSide.isLong()) {
            return averageClosePrice.percentageChangeFrom(averageOpenPrice).doubleValue();
        } else {
            return averageOpenPrice.percentageChangeFrom(averageClosePrice).doubleValue();
        }
    }

    /**
     * Calculates the daily return rate.
     *
     * @return Daily return rate as percentage
     */
    public double getDailyReturnRate() {
        if (holdingPeriodDays.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        return getROIPercentage() / holdingPeriodDays.doubleValue();
    }

    /**
     * Calculates the annualized return rate.
     *
     * @return Annualized return rate as percentage
     */
    public double getAnnualizedReturnRate() {
        return getDailyReturnRate() * 365.0;
    }

    @Override
    public String toString() {
        return String.format("PositionClosedEvent{positionId='%s', symbol=%s, side=%s, " +
                           "closed=%s, pnl=%s, roi=%.2f%%, fullyClosed=%s}",
            positionId, symbol.getCode(), positionSide, closedQuantity,
            realizedPnL, getROIPercentage(), isFullyClosed);
    }
}