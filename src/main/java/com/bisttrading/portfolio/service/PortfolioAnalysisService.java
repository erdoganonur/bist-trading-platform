package com.bisttrading.portfolio.service;

import com.bisttrading.broker.algolab.service.AlgoLabOrderService;
import com.bisttrading.entity.trading.OrderExecution;
import com.bisttrading.portfolio.dto.*;
import com.bisttrading.repository.trading.OrderExecutionRepository;
import com.bisttrading.symbol.dto.SymbolDto;
import com.bisttrading.symbol.service.SymbolService;
import com.bisttrading.trading.exception.SymbolNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Portfolio Analysis Service.
 *
 * Provides comprehensive portfolio analysis including:
 * - Position-level P&L calculations
 * - Portfolio-level aggregations
 * - Cost basis calculations from historical executions
 * - Performance metrics and rankings
 * - Sector allocation analysis
 *
 * Features:
 * - Real-time price data integration
 * - Accurate cost basis tracking with FIFO
 * - Comprehensive P&L calculations
 * - Performance analytics
 * - Caching for performance (5min TTL)
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since 2.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioAnalysisService {

    private final AlgoLabOrderService algoLabOrderService;
    private final OrderExecutionRepository executionRepository;
    private final SymbolService symbolService;

    /**
     * Gets enriched portfolio with comprehensive P&L calculations.
     *
     * This method:
     * 1. Retrieves current positions from AlgoLab
     * 2. Enriches each position with cost basis and P&L
     * 3. Calculates portfolio-level summaries
     * 4. Analyzes sector allocation
     * 5. Identifies top performers
     *
     * Results are cached for 5 minutes.
     *
     * @param userId User ID
     * @return Complete portfolio summary with all metrics
     */
    @Cacheable(value = "portfolio", key = "#userId", unless = "#result == null")
    public PortfolioSummaryDto getEnrichedPortfolio(String userId) {
        log.info("Calculating enriched portfolio for user: {}", userId);

        try {
            // 1. Get positions from AlgoLab
            Map<String, Object> positionsResponse = algoLabOrderService.getInstantPosition(null);
            List<Map<String, Object>> rawPositions = extractPositions(positionsResponse);

            if (rawPositions.isEmpty()) {
                log.info("No positions found for user: {}", userId);
                return createEmptyPortfolio();
            }

            // 2. Enrich each position with P&L calculations
            List<EnrichedPositionDto> enrichedPositions = rawPositions.stream()
                    .map(pos -> enrichPosition(userId, pos))
                    .filter(Objects::nonNull) // Filter out positions that failed to enrich
                    .collect(Collectors.toList());

            if (enrichedPositions.isEmpty()) {
                log.warn("All positions failed to enrich for user: {}", userId);
                return createEmptyPortfolio();
            }

            // 3. Calculate portfolio summary
            PortfolioSummaryDto summary = calculatePortfolioSummary(enrichedPositions);

            // 4. Add sector allocation
            summary.setSectorAllocation(calculateSectorAllocation(enrichedPositions));

            // 5. Add concentration risk
            summary.setLargestPositionPercent(calculateLargestPositionPercent(enrichedPositions, summary.getTotalValue()));

            // 6. Calculate win/loss statistics
            calculateWinLossStats(summary, enrichedPositions);

            summary.setDataSource("AlgoLab");

            log.info("Portfolio calculated successfully: {} positions, total value: {}, total P&L: {}",
                    enrichedPositions.size(), summary.getTotalValue(), summary.getTotalPnl());

            return summary;

        } catch (Exception e) {
            log.error("Failed to calculate enriched portfolio for user: {}", userId, e);
            throw new RuntimeException("Failed to calculate portfolio", e);
        }
    }

    /**
     * Enriches a single position with P&L and cost calculations.
     *
     * @param userId User ID
     * @param rawPosition Raw position data from AlgoLab
     * @return Enriched position or null if enrichment fails
     */
    private EnrichedPositionDto enrichPosition(String userId, Map<String, Object> rawPosition) {
        try {
            String symbol = (String) rawPosition.get("symbol");
            if (symbol == null) {
                log.warn("Position missing symbol, skipping");
                return null;
            }

            // Parse quantity
            BigDecimal quantity = parseDecimal(rawPosition.get("quantity"));
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Invalid quantity for symbol {}, skipping", symbol);
                return null;
            }

            // Get current price from symbol service
            Optional<SymbolDto> symbolInfoOpt = symbolService.getSymbol(symbol);
            if (symbolInfoOpt.isEmpty()) {
                log.warn("Symbol not found: {}, skipping position", symbol);
                return null;
            }

            SymbolDto symbolInfo = symbolInfoOpt.get();
            BigDecimal currentPrice = symbolInfo.getLastPrice();
            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Invalid current price for symbol: {}, skipping", symbol);
                return null;
            }

            // Calculate cost basis from historical executions
            CostBasisResult costBasis = calculateCostBasis(userId, symbol);

            // Calculate market value
            BigDecimal marketValue = quantity.multiply(currentPrice).setScale(2, RoundingMode.HALF_UP);

            // Calculate unrealized P&L
            BigDecimal unrealizedPnl = marketValue.subtract(costBasis.getTotalCost());
            BigDecimal unrealizedPnlPercent = BigDecimal.ZERO;
            if (costBasis.getTotalCost().compareTo(BigDecimal.ZERO) > 0) {
                unrealizedPnlPercent = unrealizedPnl
                        .divide(costBasis.getTotalCost(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }

            // Calculate today's change
            BigDecimal dayOpen = symbolInfo.getDayOpen();
            BigDecimal dayChange = BigDecimal.ZERO;
            BigDecimal dayChangePercent = BigDecimal.ZERO;

            if (dayOpen != null && dayOpen.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal priceChange = currentPrice.subtract(dayOpen);
                dayChange = priceChange.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
                dayChangePercent = priceChange
                        .divide(dayOpen, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }

            return EnrichedPositionDto.builder()
                    .symbol(symbol)
                    .symbolName(symbolInfo.getName())
                    .sector(symbolInfo.getSector())
                    .quantity(quantity)
                    .averageCost(costBasis.getAverageCost())
                    .totalCost(costBasis.getTotalCost())
                    .currentPrice(currentPrice)
                    .marketValue(marketValue)
                    .unrealizedPnl(unrealizedPnl)
                    .unrealizedPnlPercent(unrealizedPnlPercent)
                    .dayChange(dayChange)
                    .dayChangePercent(dayChangePercent)
                    .totalCommission(costBasis.getTotalCommission())
                    .totalTax(costBasis.getTotalTax())
                    .lastUpdated(Instant.now())
                    .build();

        } catch (Exception e) {
            log.error("Failed to enrich position", e);
            return null;
        }
    }

    /**
     * Calculates cost basis from historical executions using FIFO method.
     *
     * @param userId User ID
     * @param symbol Symbol code
     * @return Cost basis result
     */
    private CostBasisResult calculateCostBasis(String userId, String symbol) {
        try {
            // Get all BUY executions for this symbol
            List<OrderExecution> executions = executionRepository
                    .findBuyExecutionsForUserAndSymbol(userId, symbol);

            if (executions == null || executions.isEmpty()) {
                log.debug("No executions found for user {} and symbol {}, using zero cost basis", userId, symbol);
                return CostBasisResult.empty();
            }

            BigDecimal totalQuantity = BigDecimal.ZERO;
            BigDecimal totalCost = BigDecimal.ZERO;
            BigDecimal totalCommission = BigDecimal.ZERO;
            BigDecimal totalTax = BigDecimal.ZERO;

            for (OrderExecution exec : executions) {
                BigDecimal execQuantity = BigDecimal.valueOf(exec.getExecutionQuantity());
                BigDecimal execPrice = exec.getExecutionPrice();
                BigDecimal execCost = execQuantity.multiply(execPrice);

                totalQuantity = totalQuantity.add(execQuantity);
                totalCost = totalCost.add(execCost);
                totalCommission = totalCommission.add(exec.getCommission() != null ? exec.getCommission() : BigDecimal.ZERO);
                totalTax = totalTax.add(exec.getTaxAmount() != null ? exec.getTaxAmount() : BigDecimal.ZERO);
            }

            BigDecimal averageCost = BigDecimal.ZERO;
            if (totalQuantity.compareTo(BigDecimal.ZERO) > 0) {
                averageCost = totalCost.divide(totalQuantity, 4, RoundingMode.HALF_UP);
            }

            return CostBasisResult.builder()
                    .averageCost(averageCost)
                    .totalCost(totalCost)
                    .totalCommission(totalCommission)
                    .totalTax(totalTax)
                    .build();

        } catch (Exception e) {
            log.error("Failed to calculate cost basis for user {} and symbol {}", userId, symbol, e);
            return CostBasisResult.empty();
        }
    }

    /**
     * Calculates portfolio-level summary from enriched positions.
     *
     * @param positions List of enriched positions
     * @return Portfolio summary
     */
    private PortfolioSummaryDto calculatePortfolioSummary(List<EnrichedPositionDto> positions) {
        // Calculate totals
        BigDecimal totalValue = positions.stream()
                .map(EnrichedPositionDto::getMarketValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCost = positions.stream()
                .map(EnrichedPositionDto::getTotalCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPnl = positions.stream()
                .map(EnrichedPositionDto::getUnrealizedPnl)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPnlPercent = BigDecimal.ZERO;
        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            totalPnlPercent = totalPnl
                    .divide(totalCost, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        // Calculate day P&L
        BigDecimal dayPnl = positions.stream()
                .map(EnrichedPositionDto::getDayChange)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal previousValue = totalValue.subtract(dayPnl);
        BigDecimal dayPnlPercent = BigDecimal.ZERO;
        if (previousValue.compareTo(BigDecimal.ZERO) > 0) {
            dayPnlPercent = dayPnl
                    .divide(previousValue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        // Calculate total fees
        BigDecimal totalCommission = positions.stream()
                .map(EnrichedPositionDto::getTotalCommission)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalTax = positions.stream()
                .map(EnrichedPositionDto::getTotalTax)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalFees = totalCommission.add(totalTax);

        // Find top gainers and losers
        List<PositionPerformance> topGainers = positions.stream()
                .sorted((a, b) -> b.getUnrealizedPnlPercent().compareTo(a.getUnrealizedPnlPercent()))
                .limit(3)
                .map(this::toPositionPerformance)
                .collect(Collectors.toList());

        List<PositionPerformance> topLosers = positions.stream()
                .sorted(Comparator.comparing(EnrichedPositionDto::getUnrealizedPnlPercent))
                .limit(3)
                .map(this::toPositionPerformance)
                .collect(Collectors.toList());

        // Find best and worst performers today
        PositionPerformance bestToday = positions.stream()
                .max(Comparator.comparing(EnrichedPositionDto::getDayChangePercent))
                .map(this::toPositionPerformance)
                .orElse(null);

        PositionPerformance worstToday = positions.stream()
                .min(Comparator.comparing(EnrichedPositionDto::getDayChangePercent))
                .map(this::toPositionPerformance)
                .orElse(null);

        return PortfolioSummaryDto.builder()
                .positions(positions)
                .positionCount(positions.size())
                .totalValue(totalValue)
                .totalCost(totalCost)
                .totalPnl(totalPnl)
                .totalPnlPercent(totalPnlPercent)
                .dayPnl(dayPnl)
                .dayPnlPercent(dayPnlPercent)
                .totalFees(totalFees)
                .totalCommission(totalCommission)
                .totalTax(totalTax)
                .topGainers(topGainers)
                .topLosers(topLosers)
                .bestToday(bestToday)
                .worstToday(worstToday)
                .lastUpdated(Instant.now())
                .build();
    }

    /**
     * Calculates sector allocation breakdown.
     *
     * @param positions List of enriched positions
     * @return Map of sector allocations
     */
    private Map<String, SectorAllocation> calculateSectorAllocation(List<EnrichedPositionDto> positions) {
        BigDecimal totalValue = positions.stream()
                .map(EnrichedPositionDto::getMarketValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return Collections.emptyMap();
        }

        // Group by sector
        Map<String, List<EnrichedPositionDto>> bySector = positions.stream()
                .collect(Collectors.groupingBy(
                        pos -> pos.getSector() != null ? pos.getSector() : "Other"
                ));

        // Calculate allocation for each sector
        return bySector.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            BigDecimal sectorValue = entry.getValue().stream()
                                    .map(EnrichedPositionDto::getMarketValue)
                                    .filter(Objects::nonNull)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                            BigDecimal percentage = sectorValue
                                    .divide(totalValue, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100));

                            return SectorAllocation.builder()
                                    .sector(entry.getKey())
                                    .value(sectorValue)
                                    .percentage(percentage)
                                    .positionCount(entry.getValue().size())
                                    .build();
                        }
                ));
    }

    /**
     * Calculates concentration risk (largest position as % of portfolio).
     *
     * @param positions List of positions
     * @param totalValue Total portfolio value
     * @return Largest position percentage
     */
    private BigDecimal calculateLargestPositionPercent(List<EnrichedPositionDto> positions, BigDecimal totalValue) {
        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return positions.stream()
                .map(EnrichedPositionDto::getMarketValue)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .map(maxValue -> maxValue
                        .divide(totalValue, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)))
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Calculates win/loss statistics for the portfolio.
     *
     * @param summary Portfolio summary to update
     * @param positions List of positions
     */
    private void calculateWinLossStats(PortfolioSummaryDto summary, List<EnrichedPositionDto> positions) {
        int winning = (int) positions.stream()
                .filter(EnrichedPositionDto::isProfitable)
                .count();

        int losing = (int) positions.stream()
                .filter(pos -> !pos.isProfitable())
                .count();

        BigDecimal winRate = BigDecimal.ZERO;
        if (positions.size() > 0) {
            winRate = BigDecimal.valueOf(winning)
                    .divide(BigDecimal.valueOf(positions.size()), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        summary.setWinningPositions(winning);
        summary.setLosingPositions(losing);
        summary.setWinRate(winRate);
    }

    /**
     * Convert position to performance DTO.
     *
     * @param position Enriched position
     * @return Position performance DTO
     */
    private PositionPerformance toPositionPerformance(EnrichedPositionDto position) {
        return PositionPerformance.builder()
                .symbol(position.getSymbol())
                .symbolName(position.getSymbolName())
                .unrealizedPnl(position.getUnrealizedPnl())
                .unrealizedPnlPercent(position.getUnrealizedPnlPercent())
                .marketValue(position.getMarketValue())
                .dayChangePercent(position.getDayChangePercent())
                .build();
    }

    /**
     * Extract positions from AlgoLab response.
     *
     * @param response AlgoLab API response
     * @return List of position maps
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractPositions(Map<String, Object> response) {
        if (response == null) {
            return Collections.emptyList();
        }

        boolean success = (boolean) response.getOrDefault("success", false);
        if (!success) {
            log.warn("AlgoLab positions request failed: {}", response.get("message"));
            return Collections.emptyList();
        }

        Object content = response.get("content");
        if (content instanceof List) {
            return (List<Map<String, Object>>) content;
        } else if (content instanceof Map) {
            Map<String, Object> contentMap = (Map<String, Object>) content;
            Object positions = contentMap.get("positions");
            if (positions instanceof List) {
                return (List<Map<String, Object>>) positions;
            }
        }

        return Collections.emptyList();
    }

    /**
     * Create empty portfolio for users with no positions.
     *
     * @return Empty portfolio summary
     */
    private PortfolioSummaryDto createEmptyPortfolio() {
        return PortfolioSummaryDto.builder()
                .positions(Collections.emptyList())
                .positionCount(0)
                .totalValue(BigDecimal.ZERO)
                .totalCost(BigDecimal.ZERO)
                .totalPnl(BigDecimal.ZERO)
                .totalPnlPercent(BigDecimal.ZERO)
                .dayPnl(BigDecimal.ZERO)
                .dayPnlPercent(BigDecimal.ZERO)
                .totalFees(BigDecimal.ZERO)
                .totalCommission(BigDecimal.ZERO)
                .totalTax(BigDecimal.ZERO)
                .topGainers(Collections.emptyList())
                .topLosers(Collections.emptyList())
                .sectorAllocation(Collections.emptyMap())
                .winningPositions(0)
                .losingPositions(0)
                .winRate(BigDecimal.ZERO)
                .largestPositionPercent(BigDecimal.ZERO)
                .lastUpdated(Instant.now())
                .dataSource("AlgoLab")
                .build();
    }

    /**
     * Parse decimal from object safely.
     *
     * @param value Object to parse
     * @return BigDecimal or null
     */
    private BigDecimal parseDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        if (value instanceof String) {
            try {
                return new BigDecimal((String) value);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse decimal from string: {}", value);
                return null;
            }
        }
        return null;
    }
}
