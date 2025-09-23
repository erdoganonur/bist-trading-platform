package com.bisttrading.core.domain.specifications;

import com.bisttrading.core.domain.valueobjects.*;
import com.bisttrading.core.domain.events.OrderCreatedEvent;
import com.bisttrading.core.domain.services.TradingService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Specifications for order validation and business rules.
 * Encapsulates various business rules that orders must satisfy.
 */
public class OrderSpecification {

    /**
     * Specification for valid order quantity.
     */
    public static class ValidQuantitySpecification implements Specification<OrderRequest> {
        @Override
        public boolean isSatisfiedBy(OrderRequest order) {
            return order.quantity().isPositive() &&
                   order.quantity().isValidForTrading() &&
                   order.quantity().getValue().compareTo(BigDecimal.valueOf(order.symbol().getMinimumLotSize())) >= 0;
        }
    }

    /**
     * Specification for valid order price.
     */
    public static class ValidPriceSpecification implements Specification<OrderRequest> {
        @Override
        public boolean isSatisfiedBy(OrderRequest order) {
            // Market orders don't need price validation
            if (order.orderType() == OrderCreatedEvent.OrderType.MARKET) {
                return true;
            }

            // Limit orders must have valid price
            return order.price() != null &&
                   order.price().getValue().compareTo(BigDecimal.ZERO) > 0 &&
                   order.price().getSymbol().equals(order.symbol().getCode());
        }
    }

    /**
     * Specification for trading hours.
     */
    public static class TradingHoursSpecification implements Specification<OrderRequest> {
        @Override
        public boolean isSatisfiedBy(OrderRequest order) {
            LocalDateTime now = LocalDateTime.now();
            LocalTime currentTime = now.toLocalTime();

            // BIST trading hours: 09:30 - 18:00 on weekdays
            if (order.symbol().isBistSymbol()) {
                LocalTime marketOpen = LocalTime.of(9, 30);
                LocalTime marketClose = LocalTime.of(18, 0);

                return now.getDayOfWeek().getValue() <= 5 && // Monday to Friday
                       currentTime.isAfter(marketOpen) &&
                       currentTime.isBefore(marketClose);
            }

            // Other exchanges might have different hours
            return true;
        }
    }

    /**
     * Specification for sufficient balance.
     */
    public static class SufficientBalanceSpecification implements Specification<OrderRequestWithBalance> {
        @Override
        public boolean isSatisfiedBy(OrderRequestWithBalance orderWithBalance) {
            OrderRequest order = orderWithBalance.orderRequest();
            Money availableBalance = orderWithBalance.availableBalance();

            if (order.orderSide() == OrderCreatedEvent.OrderSide.SELL) {
                // For sell orders, check if user has enough shares
                return true; // This would typically check position holdings
            }

            // For buy orders, calculate required amount
            Money requiredAmount;
            if (order.orderType() == OrderCreatedEvent.OrderType.MARKET) {
                // For market orders, estimate with current price + buffer
                requiredAmount = estimateMarketOrderValue(order);
            } else {
                // For limit orders, calculate exact amount
                requiredAmount = order.price().toMoney()
                    .multiply(order.quantity().getValue());
            }

            return availableBalance.isGreaterThanOrEqual(requiredAmount);
        }

        private Money estimateMarketOrderValue(OrderRequest order) {
            // This would typically use current market price with a safety buffer
            // For now, return a conservative estimate
            return Money.of(1000000, Currency.TRY); // 1M TRY as placeholder
        }
    }

    /**
     * Specification for valid symbol status.
     */
    public static class ActiveSymbolSpecification implements Specification<OrderRequest> {
        @Override
        public boolean isSatisfiedBy(OrderRequest order) {
            return order.symbol().isActive() &&
                   order.symbol().isValidForTrading() &&
                   !order.symbol().hasSpecialTradingRules(); // Or handle special rules
        }
    }

    /**
     * Specification for price limits.
     */
    public static class PriceLimitSpecification implements Specification<OrderRequestWithRefPrice> {
        @Override
        public boolean isSatisfiedBy(OrderRequestWithRefPrice orderWithRef) {
            OrderRequest order = orderWithRef.orderRequest();
            Price referencePrice = orderWithRef.referencePrice();

            if (order.orderType() == OrderCreatedEvent.OrderType.MARKET) {
                return true;
            }

            double limitPercentage = order.symbol().getDailyLimitPercentage();
            return order.price().isWithinDailyLimits(referencePrice, BigDecimal.valueOf(limitPercentage));
        }
    }

    /**
     * Specification for minimum order value.
     */
    public static class MinimumOrderValueSpecification implements Specification<OrderRequest> {
        private static final Money MINIMUM_ORDER_VALUE = Money.of(100, Currency.TRY); // 100 TRY minimum

        @Override
        public boolean isSatisfiedBy(OrderRequest order) {
            if (order.orderType() == OrderCreatedEvent.OrderType.MARKET) {
                return true; // Can't validate market order value without current price
            }

            Money orderValue = order.price().toMoney()
                .multiply(order.quantity().getValue());
            return orderValue.isGreaterThanOrEqual(MINIMUM_ORDER_VALUE);
        }
    }

    /**
     * Specification for risk limits.
     */
    public static class RiskLimitSpecification implements Specification<OrderRequestWithRisk> {
        @Override
        public boolean isSatisfiedBy(OrderRequestWithRisk orderWithRisk) {
            OrderRequest order = orderWithRisk.orderRequest();
            Money accountValue = orderWithRisk.accountValue();
            BigDecimal maxPositionRisk = orderWithRisk.maxPositionRisk();

            Money orderValue;
            if (order.orderType() == OrderCreatedEvent.OrderType.MARKET) {
                orderValue = Money.of(1000000, Currency.TRY); // Conservative estimate
            } else {
                orderValue = order.price().toMoney()
                    .multiply(order.quantity().getValue());
            }

            BigDecimal positionRiskPercentage = orderValue.getAmount()
                .divide(accountValue.getAmount(), 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

            return positionRiskPercentage.compareTo(maxPositionRisk) <= 0;
        }
    }

    /**
     * Composite specification for complete order validation.
     */
    public static class CompleteOrderValidationSpecification implements Specification<CompleteOrderContext> {
        @Override
        public boolean isSatisfiedBy(CompleteOrderContext context) {
            return new ValidQuantitySpecification()
                .and(new ValidPriceSpecification())
                .and(new TradingHoursSpecification())
                .and(new ActiveSymbolSpecification())
                .isSatisfiedBy(context.orderRequest())
                &&
                new SufficientBalanceSpecification()
                    .isSatisfiedBy(new OrderRequestWithBalance(context.orderRequest(), context.availableBalance()))
                &&
                new PriceLimitSpecification()
                    .isSatisfiedBy(new OrderRequestWithRefPrice(context.orderRequest(), context.referencePrice()))
                &&
                new MinimumOrderValueSpecification()
                    .isSatisfiedBy(context.orderRequest())
                &&
                new RiskLimitSpecification()
                    .isSatisfiedBy(new OrderRequestWithRisk(context.orderRequest(),
                        context.accountValue(), context.maxPositionRisk()));
        }
    }

    // Data records for specification contexts

    /**
     * Basic order request data.
     */
    public record OrderRequest(
        Symbol symbol,
        OrderCreatedEvent.OrderType orderType,
        OrderCreatedEvent.OrderSide orderSide,
        Quantity quantity,
        Price price
    ) {}

    /**
     * Order request with balance information.
     */
    public record OrderRequestWithBalance(
        OrderRequest orderRequest,
        Money availableBalance
    ) {}

    /**
     * Order request with reference price for limit validation.
     */
    public record OrderRequestWithRefPrice(
        OrderRequest orderRequest,
        Price referencePrice
    ) {}

    /**
     * Order request with risk information.
     */
    public record OrderRequestWithRisk(
        OrderRequest orderRequest,
        Money accountValue,
        BigDecimal maxPositionRisk
    ) {}

    /**
     * Complete order context with all validation data.
     */
    public record CompleteOrderContext(
        OrderRequest orderRequest,
        Money availableBalance,
        Price referencePrice,
        Money accountValue,
        BigDecimal maxPositionRisk,
        TradingService.MarketStatus marketStatus
    ) {}
}