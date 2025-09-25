package com.bisttrading.broker.service;

import com.bisttrading.broker.algolab.model.OrderRequest;
import com.bisttrading.broker.algolab.model.OrderSide;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskManagementService {

    @Value("${trading.risk.max-daily-loss:50000.0}")
    private BigDecimal maxDailyLoss;

    @Value("${trading.risk.max-position-size:100000.0}")
    private BigDecimal maxPositionSize;

    @Value("${trading.risk.max-order-value:20000.0}")
    private BigDecimal maxOrderValue;

    @Value("${trading.risk.max-leverage:3.0}")
    private BigDecimal maxLeverage;

    @Value("${trading.risk.concentration-limit:0.1}")
    private BigDecimal concentrationLimit; // 10% of portfolio

    public CompletableFuture<RiskResult> validateOrderRisk(OrderRequest orderRequest) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Validating risk for order: {} {} {} at {}",
                        orderRequest.getSide(), orderRequest.getQuantity(),
                        orderRequest.getSymbol(), orderRequest.getPrice());

                // Check order value limits
                RiskResult orderValueCheck = checkOrderValueLimit(orderRequest);
                if (!orderValueCheck.isApproved()) {
                    return orderValueCheck;
                }

                // Check position size limits
                RiskResult positionSizeCheck = checkPositionSizeLimit(orderRequest);
                if (!positionSizeCheck.isApproved()) {
                    return positionSizeCheck;
                }

                // Check daily loss limits
                RiskResult dailyLossCheck = checkDailyLossLimit(orderRequest);
                if (!dailyLossCheck.isApproved()) {
                    return dailyLossCheck;
                }

                // Check concentration limits
                RiskResult concentrationCheck = checkConcentrationLimit(orderRequest);
                if (!concentrationCheck.isApproved()) {
                    return concentrationCheck;
                }

                // Check leverage limits
                RiskResult leverageCheck = checkLeverageLimit(orderRequest);
                if (!leverageCheck.isApproved()) {
                    return leverageCheck;
                }

                log.debug("Risk validation passed for order: {}", orderRequest.getClientOrderId());
                return RiskResult.approved();

            } catch (Exception e) {
                log.error("Risk validation error for order: {}", orderRequest.getClientOrderId(), e);
                return RiskResult.rejected("Risk validation error: " + e.getMessage());
            }
        });
    }

    private RiskResult checkOrderValueLimit(OrderRequest orderRequest) {
        if (orderRequest.getPrice() != null && orderRequest.getQuantity() != null) {
            BigDecimal orderValue = orderRequest.getPrice()
                    .multiply(BigDecimal.valueOf(orderRequest.getQuantity()));

            if (orderValue.compareTo(maxOrderValue) > 0) {
                return RiskResult.rejected(
                    String.format("Order value %s exceeds maximum allowed %s",
                                  orderValue, maxOrderValue));
            }
        }
        return RiskResult.approved();
    }

    private RiskResult checkPositionSizeLimit(OrderRequest orderRequest) {
        // In a real implementation, this would check existing positions
        // and calculate the new position size after the order

        // Placeholder: assume current position is 0
        BigDecimal currentPosition = BigDecimal.ZERO;

        if (orderRequest.getPrice() != null && orderRequest.getQuantity() != null) {
            BigDecimal orderValue = orderRequest.getPrice()
                    .multiply(BigDecimal.valueOf(orderRequest.getQuantity()));

            BigDecimal newPositionSize = orderRequest.getSide() == OrderSide.BUY
                ? currentPosition.add(orderValue)
                : currentPosition.subtract(orderValue);

            if (newPositionSize.abs().compareTo(maxPositionSize) > 0) {
                return RiskResult.rejected(
                    String.format("Position size %s would exceed maximum %s",
                                  newPositionSize.abs(), maxPositionSize));
            }
        }
        return RiskResult.approved();
    }

    private RiskResult checkDailyLossLimit(OrderRequest orderRequest) {
        // In a real implementation, this would check the current daily P&L
        // and estimate if this order could exceed the daily loss limit

        // Placeholder: assume current daily loss is 0
        BigDecimal currentDailyLoss = BigDecimal.ZERO;

        // For stop orders, calculate potential loss
        if (orderRequest.getStopPrice() != null && orderRequest.getPrice() != null) {
            BigDecimal potentialLoss = calculatePotentialLoss(orderRequest);

            if (currentDailyLoss.add(potentialLoss).compareTo(maxDailyLoss) > 0) {
                return RiskResult.rejected(
                    String.format("Potential daily loss %s would exceed limit %s",
                                  currentDailyLoss.add(potentialLoss), maxDailyLoss));
            }
        }

        return RiskResult.approved();
    }

    private RiskResult checkConcentrationLimit(OrderRequest orderRequest) {
        // In a real implementation, this would check the portfolio concentration
        // to ensure no single symbol exceeds the concentration limit

        // Placeholder: assume portfolio value is 100k and symbol concentration is 5%
        BigDecimal portfolioValue = BigDecimal.valueOf(100000);
        BigDecimal currentSymbolValue = portfolioValue.multiply(BigDecimal.valueOf(0.05));

        if (orderRequest.getPrice() != null && orderRequest.getQuantity() != null) {
            BigDecimal orderValue = orderRequest.getPrice()
                    .multiply(BigDecimal.valueOf(orderRequest.getQuantity()));

            BigDecimal newSymbolValue = orderRequest.getSide() == OrderSide.BUY
                ? currentSymbolValue.add(orderValue)
                : currentSymbolValue.subtract(orderValue);

            BigDecimal concentration = newSymbolValue.divide(portfolioValue, 4, BigDecimal.ROUND_HALF_UP);

            if (concentration.compareTo(concentrationLimit) > 0) {
                return RiskResult.rejected(
                    String.format("Symbol concentration %.2f%% would exceed limit %.2f%%",
                                  concentration.multiply(BigDecimal.valueOf(100)),
                                  concentrationLimit.multiply(BigDecimal.valueOf(100))));
            }
        }

        return RiskResult.approved();
    }

    private RiskResult checkLeverageLimit(OrderRequest orderRequest) {
        // Check if margin trading and leverage limits
        if (Boolean.TRUE.equals(orderRequest.getMarginBuy()) ||
            Boolean.TRUE.equals(orderRequest.getShortSale())) {

            // In a real implementation, calculate current leverage
            // Placeholder: assume current leverage is 1.5
            BigDecimal currentLeverage = BigDecimal.valueOf(1.5);

            if (currentLeverage.compareTo(maxLeverage) > 0) {
                return RiskResult.rejected(
                    String.format("Current leverage %.2f exceeds maximum %.2f",
                                  currentLeverage, maxLeverage));
            }
        }

        return RiskResult.approved();
    }

    private BigDecimal calculatePotentialLoss(OrderRequest orderRequest) {
        if (orderRequest.getPrice() == null || orderRequest.getStopPrice() == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal priceDiff = orderRequest.getSide() == OrderSide.BUY
            ? orderRequest.getPrice().subtract(orderRequest.getStopPrice())
            : orderRequest.getStopPrice().subtract(orderRequest.getPrice());

        return priceDiff.multiply(BigDecimal.valueOf(orderRequest.getQuantity())).abs();
    }

    @lombok.Data
    @lombok.Builder
    public static class RiskResult {
        private boolean approved;
        private String reason;
        private String riskLevel;

        public static RiskResult approved() {
            return RiskResult.builder()
                .approved(true)
                .riskLevel("LOW")
                .build();
        }

        public static RiskResult rejected(String reason) {
            return RiskResult.builder()
                .approved(false)
                .reason(reason)
                .riskLevel("HIGH")
                .build();
        }
    }
}