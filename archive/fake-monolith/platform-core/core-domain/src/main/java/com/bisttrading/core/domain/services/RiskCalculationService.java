package com.bisttrading.core.domain.services;

import com.bisttrading.core.domain.valueobjects.*;
import com.bisttrading.core.domain.events.PositionOpenedEvent;

import java.math.BigDecimal;
import java.util.List;

/**
 * Domain service interface for risk calculation and management.
 * Encapsulates business logic related to trading risk assessment.
 */
public interface RiskCalculationService {

    /**
     * Calculates portfolio risk metrics.
     *
     * @param positions      Current positions
     * @param accountValue   Total account value
     * @param marketPrices   Current market prices for symbols
     * @return Portfolio risk metrics
     */
    PortfolioRisk calculatePortfolioRisk(List<Position> positions, Money accountValue,
                                       List<MarketPrice> marketPrices);

    /**
     * Calculates Value at Risk (VaR) for a portfolio.
     *
     * @param positions       Current positions
     * @param accountValue    Total account value
     * @param confidenceLevel Confidence level (e.g., 0.95 for 95%)
     * @param timeHorizon     Time horizon in days
     * @return VaR amount
     */
    Money calculateVaR(List<Position> positions, Money accountValue, BigDecimal confidenceLevel,
                      int timeHorizon);

    /**
     * Calculates position-level risk for a specific position.
     *
     * @param position     The position to analyze
     * @param marketPrice  Current market price
     * @param accountValue Total account value
     * @return Position risk metrics
     */
    PositionRisk calculatePositionRisk(Position position, Price marketPrice, Money accountValue);

    /**
     * Calculates correlation risk between positions.
     *
     * @param positions Current positions
     * @return Correlation risk metrics
     */
    CorrelationRisk calculateCorrelationRisk(List<Position> positions);

    /**
     * Calculates margin requirement for a position.
     *
     * @param symbol       Trading symbol
     * @param positionSide Long or short
     * @param quantity     Position quantity
     * @param price        Position price
     * @return Margin requirement
     */
    Money calculateMarginRequirement(Symbol symbol, PositionOpenedEvent.PositionSide positionSide,
                                   Quantity quantity, Price price);

    /**
     * Calculates maximum position size based on risk limits.
     *
     * @param symbol         Trading symbol
     * @param accountValue   Total account value
     * @param riskLimit      Maximum risk percentage
     * @param stopLossPrice  Stop loss price
     * @param entryPrice     Entry price
     * @return Maximum safe position size
     */
    Quantity calculateMaxPositionSize(Symbol symbol, Money accountValue, BigDecimal riskLimit,
                                    Price stopLossPrice, Price entryPrice);

    /**
     * Validates if adding a position would exceed risk limits.
     *
     * @param newPosition     Proposed new position
     * @param existingPositions Current positions
     * @param accountValue    Total account value
     * @param riskLimits      Risk limit configuration
     * @return Risk validation result
     */
    RiskValidationResult validateRiskLimits(Position newPosition, List<Position> existingPositions,
                                          Money accountValue, RiskLimits riskLimits);

    /**
     * Calculates leverage ratio for a position.
     *
     * @param positionValue Position value
     * @param equity        Available equity
     * @return Leverage ratio
     */
    BigDecimal calculateLeverageRatio(Money positionValue, Money equity);

    /**
     * Calculates beta (systematic risk) for a symbol.
     *
     * @param symbol     Trading symbol
     * @param benchmark  Benchmark symbol (e.g., XU100)
     * @param period     Analysis period in days
     * @return Beta coefficient
     */
    BigDecimal calculateBeta(Symbol symbol, Symbol benchmark, int period);

    /**
     * Portfolio risk metrics.
     */
    record PortfolioRisk(
        Money totalValue,
        Money totalRisk,
        BigDecimal riskPercentage,
        BigDecimal diversificationRatio,
        BigDecimal sharpeRatio,
        BigDecimal maxDrawdown,
        String riskCategory,
        List<String> riskWarnings
    ) {}

    /**
     * Position-level risk metrics.
     */
    record PositionRisk(
        String positionId,
        Symbol symbol,
        Money positionValue,
        Money riskAmount,
        BigDecimal riskPercentage,
        BigDecimal beta,
        BigDecimal volatility,
        Money potentialLoss,
        String riskLevel
    ) {}

    /**
     * Correlation risk metrics.
     */
    record CorrelationRisk(
        BigDecimal averageCorrelation,
        BigDecimal maxCorrelation,
        List<SymbolCorrelation> correlations,
        BigDecimal concentrationRisk,
        List<String> warnings
    ) {}

    /**
     * Symbol correlation data.
     */
    record SymbolCorrelation(
        Symbol symbol1,
        Symbol symbol2,
        BigDecimal correlation,
        String significance
    ) {}

    /**
     * Position data for risk calculations.
     */
    record Position(
        String positionId,
        Symbol symbol,
        PositionOpenedEvent.PositionSide side,
        Quantity quantity,
        Price averagePrice,
        Money currentValue,
        Money unrealizedPnL
    ) {}

    /**
     * Market price data.
     */
    record MarketPrice(
        Symbol symbol,
        Price currentPrice,
        Price dayHigh,
        Price dayLow,
        BigDecimal volatility
    ) {}

    /**
     * Risk limits configuration.
     */
    record RiskLimits(
        BigDecimal maxPortfolioRisk,
        BigDecimal maxPositionRisk,
        BigDecimal maxSectorConcentration,
        BigDecimal maxLeverage,
        Money maxPositionValue,
        BigDecimal maxCorrelation
    ) {
        public static RiskLimits conservative() {
            return new RiskLimits(
                new BigDecimal("5.0"),    // 5% max portfolio risk
                new BigDecimal("2.0"),    // 2% max position risk
                new BigDecimal("20.0"),   // 20% max sector concentration
                new BigDecimal("2.0"),    // 2x max leverage
                Money.of(50000, Currency.TRY), // 50k TRY max position
                new BigDecimal("0.7")     // 0.7 max correlation
            );
        }

        public static RiskLimits moderate() {
            return new RiskLimits(
                new BigDecimal("10.0"),
                new BigDecimal("5.0"),
                new BigDecimal("30.0"),
                new BigDecimal("3.0"),
                Money.of(100000, Currency.TRY),
                new BigDecimal("0.8")
            );
        }

        public static RiskLimits aggressive() {
            return new RiskLimits(
                new BigDecimal("20.0"),
                new BigDecimal("10.0"),
                new BigDecimal("50.0"),
                new BigDecimal("5.0"),
                Money.of(500000, Currency.TRY),
                new BigDecimal("0.9")
            );
        }
    }

    /**
     * Risk validation result.
     */
    record RiskValidationResult(
        boolean isValid,
        String errorMessage,
        List<String> warnings,
        List<RiskViolation> violations,
        BigDecimal newPortfolioRisk,
        BigDecimal newLeverage
    ) {
        public static RiskValidationResult valid(BigDecimal newPortfolioRisk, BigDecimal newLeverage) {
            return new RiskValidationResult(true, null, List.of(), List.of(),
                newPortfolioRisk, newLeverage);
        }

        public static RiskValidationResult validWithWarnings(BigDecimal newPortfolioRisk, BigDecimal newLeverage,
                                                           List<String> warnings) {
            return new RiskValidationResult(true, null, warnings, List.of(),
                newPortfolioRisk, newLeverage);
        }

        public static RiskValidationResult invalid(String errorMessage, List<RiskViolation> violations) {
            return new RiskValidationResult(false, errorMessage, List.of(), violations, null, null);
        }
    }

    /**
     * Risk violation details.
     */
    record RiskViolation(
        String violationType,
        String description,
        BigDecimal currentValue,
        BigDecimal limitValue,
        String severity
    ) {}
}