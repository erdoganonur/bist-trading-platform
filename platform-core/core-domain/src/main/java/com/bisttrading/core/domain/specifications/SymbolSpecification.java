package com.bisttrading.core.domain.specifications;

import com.bisttrading.core.domain.valueobjects.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Specifications for symbol validation and business rules.
 * Encapsulates various business rules that symbols must satisfy for different operations.
 */
public class SymbolSpecification {

    /**
     * Specification for active trading symbols.
     */
    public static class ActiveTradingSpecification implements Specification<Symbol> {
        @Override
        public boolean isSatisfiedBy(Symbol symbol) {
            return symbol.isActive() &&
                   symbol.isValidForTrading() &&
                   symbol.getExchange() != null &&
                   symbol.getMarketType() != null;
        }
    }

    /**
     * Specification for BIST symbols.
     */
    public static class BistSymbolSpecification implements Specification<Symbol> {
        @Override
        public boolean isSatisfiedBy(Symbol symbol) {
            return symbol.isBistSymbol() &&
                   symbol.getCode().length() >= 4 &&
                   symbol.getCode().length() <= 6 &&
                   symbol.getCode().matches("^[A-Z]+$");
        }
    }

    /**
     * Specification for main market symbols.
     */
    public static class MainMarketSpecification implements Specification<Symbol> {
        @Override
        public boolean isSatisfiedBy(Symbol symbol) {
            return symbol.isMainMarketSymbol() &&
                   (symbol.getMarketType() == MarketType.YILDIZ_PAZAR ||
                    symbol.getMarketType() == MarketType.ANA_PAZAR);
        }
    }

    /**
     * Specification for symbols that support derivatives.
     */
    public static class DerivativesSupportSpecification implements Specification<Symbol> {
        @Override
        public boolean isSatisfiedBy(Symbol symbol) {
            return symbol.supportsDerivatives() &&
                   symbol.isBistSymbol() &&
                   symbol.isMainMarketSymbol();
        }
    }

    /**
     * Specification for symbols with sufficient liquidity.
     */
    public static class LiquiditySpecification implements Specification<SymbolWithMetrics> {
        private static final BigDecimal MIN_DAILY_VOLUME = new BigDecimal("1000000"); // 1M TRY
        private static final BigDecimal MIN_AVG_DAILY_TRADES = new BigDecimal("100");

        @Override
        public boolean isSatisfiedBy(SymbolWithMetrics symbolWithMetrics) {
            return symbolWithMetrics.averageDailyVolume().compareTo(MIN_DAILY_VOLUME) >= 0 &&
                   symbolWithMetrics.averageDailyTrades().compareTo(MIN_AVG_DAILY_TRADES) >= 0 &&
                   symbolWithMetrics.bidAskSpread().compareTo(new BigDecimal("5.0")) <= 0; // Max 5% spread
        }
    }

    /**
     * Specification for symbols with low volatility (conservative trading).
     */
    public static class LowVolatilitySpecification implements Specification<SymbolWithMetrics> {
        private static final BigDecimal MAX_VOLATILITY = new BigDecimal("2.0"); // 2% daily volatility

        @Override
        public boolean isSatisfiedBy(SymbolWithMetrics symbolWithMetrics) {
            return symbolWithMetrics.volatility().compareTo(MAX_VOLATILITY) <= 0;
        }
    }

    /**
     * Specification for symbols with high volatility (aggressive trading).
     */
    public static class HighVolatilitySpecification implements Specification<SymbolWithMetrics> {
        private static final BigDecimal MIN_VOLATILITY = new BigDecimal("3.0"); // 3% daily volatility

        @Override
        public boolean isSatisfiedBy(SymbolWithMetrics symbolWithMetrics) {
            return symbolWithMetrics.volatility().compareTo(MIN_VOLATILITY) >= 0;
        }
    }

    /**
     * Specification for symbols suitable for day trading.
     */
    public static class DayTradingSpecification implements Specification<SymbolWithMetrics> {
        @Override
        public boolean isSatisfiedBy(SymbolWithMetrics symbolWithMetrics) {
            return new ActiveTradingSpecification().isSatisfiedBy(symbolWithMetrics.symbol()) &&
                   new LiquiditySpecification().isSatisfiedBy(symbolWithMetrics) &&
                   symbolWithMetrics.volatility().compareTo(new BigDecimal("1.5")) >= 0 && // Min volatility for day trading
                   symbolWithMetrics.bidAskSpread().compareTo(new BigDecimal("2.0")) <= 0; // Max 2% spread
        }
    }

    /**
     * Specification for symbols suitable for swing trading.
     */
    public static class SwingTradingSpecification implements Specification<SymbolWithMetrics> {
        @Override
        public boolean isSatisfiedBy(SymbolWithMetrics symbolWithMetrics) {
            return new ActiveTradingSpecification().isSatisfiedBy(symbolWithMetrics.symbol()) &&
                   symbolWithMetrics.volatility().compareTo(new BigDecimal("1.0")) >= 0 && // Min volatility
                   symbolWithMetrics.volatility().compareTo(new BigDecimal("5.0")) <= 0 && // Max volatility
                   symbolWithMetrics.averageDailyVolume().compareTo(new BigDecimal("500000")) >= 0; // Min volume
        }
    }

    /**
     * Specification for symbols suitable for long-term investment.
     */
    public static class LongTermInvestmentSpecification implements Specification<SymbolWithMetrics> {
        @Override
        public boolean isSatisfiedBy(SymbolWithMetrics symbolWithMetrics) {
            return new ActiveTradingSpecification().isSatisfiedBy(symbolWithMetrics.symbol()) &&
                   new MainMarketSpecification().isSatisfiedBy(symbolWithMetrics.symbol()) &&
                   symbolWithMetrics.volatility().compareTo(new BigDecimal("3.0")) <= 0 && // Max volatility
                   symbolWithMetrics.marketCap().compareTo(new BigDecimal("1000000000")) >= 0; // Min 1B TRY market cap
        }
    }

    /**
     * Specification for symbols with special trading rules.
     */
    public static class SpecialTradingRulesSpecification implements Specification<Symbol> {
        @Override
        public boolean isSatisfiedBy(Symbol symbol) {
            return symbol.hasSpecialTradingRules() ||
                   symbol.getMarketType() == MarketType.GELISIM_PAZARI ||
                   symbol.getMarketType() == MarketType.YAPILANDIRILMIS_URUNLER;
        }
    }

    /**
     * Specification for symbols that can be used as collateral.
     */
    public static class CollateralEligibilitySpecification implements Specification<SymbolWithMetrics> {
        @Override
        public boolean isSatisfiedBy(SymbolWithMetrics symbolWithMetrics) {
            return new ActiveTradingSpecification().isSatisfiedBy(symbolWithMetrics.symbol()) &&
                   new MainMarketSpecification().isSatisfiedBy(symbolWithMetrics.symbol()) &&
                   new LiquiditySpecification().isSatisfiedBy(symbolWithMetrics) &&
                   symbolWithMetrics.volatility().compareTo(new BigDecimal("2.5")) <= 0 && // Max volatility for collateral
                   symbolWithMetrics.marketCap().compareTo(new BigDecimal("500000000")) >= 0; // Min market cap
        }
    }

    /**
     * Specification for symbols suitable for algorithmic trading.
     */
    public static class AlgorithmicTradingSpecification implements Specification<SymbolWithMetrics> {
        @Override
        public boolean isSatisfiedBy(SymbolWithMetrics symbolWithMetrics) {
            return new ActiveTradingSpecification().isSatisfiedBy(symbolWithMetrics.symbol()) &&
                   symbolWithMetrics.averageDailyTrades().compareTo(new BigDecimal("500")) >= 0 && // High trade frequency
                   symbolWithMetrics.bidAskSpread().compareTo(new BigDecimal("1.0")) <= 0 && // Tight spread
                   symbolWithMetrics.averageDailyVolume().compareTo(new BigDecimal("2000000")) >= 0; // High volume
        }
    }

    /**
     * Composite specification for complete symbol validation for trading.
     */
    public static class TradingReadySpecification implements Specification<SymbolWithMetrics> {
        @Override
        public boolean isSatisfiedBy(SymbolWithMetrics symbolWithMetrics) {
            return new ActiveTradingSpecification().isSatisfiedBy(symbolWithMetrics.symbol()) &&
                   new LiquiditySpecification().isSatisfiedBy(symbolWithMetrics) &&
                   !new SpecialTradingRulesSpecification().isSatisfiedBy(symbolWithMetrics.symbol());
        }
    }

    // Data records for specification contexts

    /**
     * Symbol with trading metrics for enhanced validation.
     */
    public record SymbolWithMetrics(
        Symbol symbol,
        BigDecimal volatility,
        BigDecimal averageDailyVolume,
        BigDecimal averageDailyTrades,
        BigDecimal bidAskSpread,
        BigDecimal marketCap,
        LocalDateTime lastUpdated
    ) {}

    /**
     * Symbol with current market data.
     */
    public record SymbolWithMarketData(
        Symbol symbol,
        Price currentPrice,
        Price dayHigh,
        Price dayLow,
        Quantity volume,
        LocalDateTime timestamp
    ) {}

    /**
     * Symbol with risk metrics.
     */
    public record SymbolWithRisk(
        Symbol symbol,
        BigDecimal beta,
        BigDecimal var,
        BigDecimal maxDrawdown,
        String riskCategory
    ) {}
}