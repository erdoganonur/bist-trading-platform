package com.bisttrading.broker.service;

import com.bisttrading.broker.algolab.AlgoLabApiClient;
import com.bisttrading.broker.algolab.model.AccountInfo;
import com.bisttrading.broker.algolab.model.Position;
import com.bisttrading.broker.service.event.PortfolioEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioSynchronizationService {

    private final AlgoLabApiClient algoLabClient;
    private final PortfolioEventPublisher eventPublisher;

    // Cache for portfolio snapshots
    private final Map<String, PortfolioSnapshot> portfolioSnapshots = new ConcurrentHashMap<>();

    // Cache for position changes
    private final Map<String, Map<String, Position>> userPositions = new ConcurrentHashMap<>();

    @Async
    public CompletableFuture<PortfolioSnapshot> synchronizePortfolio(String userId, String accountId) {
        log.info("Starting portfolio synchronization for user: {} account: {}", userId, accountId);

        return CompletableFuture
            .supplyAsync(() -> fetchAccountInfo(accountId))
            .thenCompose(accountInfo -> fetchPositions(accountId)
                .thenApply(positions -> createPortfolioSnapshot(userId, accountInfo, positions)))
            .thenApply(snapshot -> processPortfolioChanges(userId, snapshot))
            .whenComplete((snapshot, throwable) -> {
                if (throwable != null) {
                    log.error("Portfolio synchronization failed for user: {}", userId, throwable);
                } else {
                    log.debug("Portfolio synchronization completed for user: {}", userId);
                }
            });
    }

    private AccountInfo fetchAccountInfo(String accountId) {
        try {
            return algoLabClient.getAccountInfo();
        } catch (Exception e) {
            log.error("Failed to fetch account info for account: {}", accountId, e);
            throw new RuntimeException("Failed to fetch account info", e);
        }
    }

    private CompletableFuture<List<Position>> fetchPositions(String accountId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return algoLabClient.getPositions();
            } catch (Exception e) {
                log.error("Failed to fetch positions for account: {}", accountId, e);
                throw new RuntimeException("Failed to fetch positions", e);
            }
        });
    }

    private PortfolioSnapshot createPortfolioSnapshot(String userId, AccountInfo accountInfo, List<Position> positions) {
        BigDecimal totalPositionValue = positions.stream()
            .filter(pos -> pos.getMarketValue() != null)
            .map(Position::getMarketValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalUnrealizedPnl = positions.stream()
            .filter(pos -> pos.getUnrealizedPnl() != null)
            .map(Position::getUnrealizedPnl)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRealizedPnl = positions.stream()
            .filter(pos -> pos.getRealizedPnl() != null)
            .map(Position::getRealizedPnl)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PortfolioSnapshot.builder()
            .userId(userId)
            .accountId(accountInfo.getAccountId())
            .cashBalance(accountInfo.getCashBalance())
            .availableCash(accountInfo.getAvailableCash())
            .portfolioValue(accountInfo.getPortfolioValue())
            .totalEquity(accountInfo.getTotalEquity())
            .totalPositionValue(totalPositionValue)
            .totalUnrealizedPnl(totalUnrealizedPnl)
            .totalRealizedPnl(totalRealizedPnl)
            .marginUsed(accountInfo.getMarginUsed())
            .marginAvailable(accountInfo.getMarginAvailable())
            .buyingPower(accountInfo.getBuyingPower())
            .positions(positions)
            .timestamp(Instant.now())
            .build();
    }

    private PortfolioSnapshot processPortfolioChanges(String userId, PortfolioSnapshot newSnapshot) {
        PortfolioSnapshot previousSnapshot = portfolioSnapshots.get(userId);

        if (previousSnapshot != null) {
            // Compare and publish changes
            compareAndPublishAccountChanges(previousSnapshot, newSnapshot);
            compareAndPublishPositionChanges(userId, previousSnapshot.getPositions(), newSnapshot.getPositions());
        } else {
            // First time sync - publish initial state
            eventPublisher.publishPortfolioInitialized(newSnapshot);
        }

        // Update caches
        portfolioSnapshots.put(userId, newSnapshot);
        updatePositionCache(userId, newSnapshot.getPositions());

        return newSnapshot;
    }

    private void compareAndPublishAccountChanges(PortfolioSnapshot previous, PortfolioSnapshot current) {
        // Cash balance changes
        if (hasSignificantChange(previous.getCashBalance(), current.getCashBalance())) {
            eventPublisher.publishCashBalanceChanged(current.getUserId(),
                previous.getCashBalance(), current.getCashBalance());
        }

        // Portfolio value changes
        if (hasSignificantChange(previous.getPortfolioValue(), current.getPortfolioValue())) {
            eventPublisher.publishPortfolioValueChanged(current.getUserId(),
                previous.getPortfolioValue(), current.getPortfolioValue());
        }

        // P&L changes
        if (hasSignificantChange(previous.getTotalUnrealizedPnl(), current.getTotalUnrealizedPnl())) {
            eventPublisher.publishUnrealizedPnlChanged(current.getUserId(),
                previous.getTotalUnrealizedPnl(), current.getTotalUnrealizedPnl());
        }

        // Margin changes
        if (hasSignificantChange(previous.getMarginUsed(), current.getMarginUsed())) {
            eventPublisher.publishMarginUsageChanged(current.getUserId(),
                previous.getMarginUsed(), current.getMarginUsed());
        }
    }

    private void compareAndPublishPositionChanges(String userId, List<Position> previousPositions, List<Position> currentPositions) {
        Map<String, Position> previousMap = previousPositions.stream()
            .collect(Collectors.toMap(Position::getSymbol, pos -> pos));

        Map<String, Position> currentMap = currentPositions.stream()
            .collect(Collectors.toMap(Position::getSymbol, pos -> pos));

        // Check for new positions
        for (String symbol : currentMap.keySet()) {
            Position current = currentMap.get(symbol);
            Position previous = previousMap.get(symbol);

            if (previous == null) {
                eventPublisher.publishPositionOpened(userId, current);
            } else {
                compareIndividualPosition(userId, symbol, previous, current);
            }
        }

        // Check for closed positions
        for (String symbol : previousMap.keySet()) {
            if (!currentMap.containsKey(symbol)) {
                eventPublisher.publishPositionClosed(userId, previousMap.get(symbol));
            }
        }
    }

    private void compareIndividualPosition(String userId, String symbol, Position previous, Position current) {
        // Quantity changes
        if (!previous.getQuantity().equals(current.getQuantity())) {
            eventPublisher.publishPositionQuantityChanged(userId, symbol,
                previous.getQuantity(), current.getQuantity());
        }

        // Market value changes
        if (hasSignificantChange(previous.getMarketValue(), current.getMarketValue())) {
            eventPublisher.publishPositionValueChanged(userId, symbol,
                previous.getMarketValue(), current.getMarketValue());
        }

        // P&L changes
        if (hasSignificantChange(previous.getUnrealizedPnl(), current.getUnrealizedPnl())) {
            eventPublisher.publishPositionPnlChanged(userId, symbol,
                previous.getUnrealizedPnl(), current.getUnrealizedPnl());
        }
    }

    private boolean hasSignificantChange(BigDecimal previous, BigDecimal current) {
        if (previous == null && current == null) return false;
        if (previous == null || current == null) return true;

        BigDecimal difference = current.subtract(previous).abs();
        BigDecimal threshold = previous.abs().multiply(BigDecimal.valueOf(0.001)); // 0.1% threshold

        return difference.compareTo(threshold) > 0;
    }

    private void updatePositionCache(String userId, List<Position> positions) {
        Map<String, Position> positionMap = positions.stream()
            .collect(Collectors.toMap(Position::getSymbol, pos -> pos));
        userPositions.put(userId, positionMap);
    }

    // Scheduled synchronization for all active users
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void scheduledPortfolioSync() {
        log.debug("Starting scheduled portfolio synchronization for {} users", portfolioSnapshots.size());

        portfolioSnapshots.keySet().forEach(userId -> {
            try {
                // In a real implementation, you'd fetch account ID from user service
                String accountId = "ACCOUNT_" + userId; // Placeholder
                synchronizePortfolio(userId, accountId);
            } catch (Exception e) {
                log.error("Scheduled sync failed for user: {}", userId, e);
            }
        });
    }

    // Manual sync trigger
    public CompletableFuture<List<PortfolioSnapshot>> syncAllPortfolios(List<String> userIds) {
        List<CompletableFuture<PortfolioSnapshot>> futures = userIds.stream()
            .map(userId -> {
                String accountId = "ACCOUNT_" + userId; // Placeholder
                return synchronizePortfolio(userId, accountId);
            })
            .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));
    }

    // Get current portfolio snapshot
    public PortfolioSnapshot getPortfolioSnapshot(String userId) {
        return portfolioSnapshots.get(userId);
    }

    // Get current positions for user
    public Map<String, Position> getCurrentPositions(String userId) {
        return userPositions.get(userId);
    }

    // Portfolio performance calculation
    public PortfolioPerformance calculatePerformance(String userId, Instant fromDate) {
        PortfolioSnapshot current = portfolioSnapshots.get(userId);
        if (current == null) {
            return null;
        }

        // In a real implementation, you'd fetch historical data
        // This is a simplified calculation
        return PortfolioPerformance.builder()
            .userId(userId)
            .fromDate(fromDate)
            .toDate(current.getTimestamp())
            .totalReturn(current.getTotalUnrealizedPnl())
            .totalReturnPercentage(calculateReturnPercentage(current))
            .build();
    }

    private BigDecimal calculateReturnPercentage(PortfolioSnapshot snapshot) {
        if (snapshot.getPortfolioValue() != null &&
            snapshot.getPortfolioValue().compareTo(BigDecimal.ZERO) > 0 &&
            snapshot.getTotalUnrealizedPnl() != null) {

            return snapshot.getTotalUnrealizedPnl()
                .divide(snapshot.getPortfolioValue(), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        }
        return BigDecimal.ZERO;
    }

    @lombok.Data
    @lombok.Builder
    public static class PortfolioSnapshot {
        private String userId;
        private String accountId;
        private BigDecimal cashBalance;
        private BigDecimal availableCash;
        private BigDecimal portfolioValue;
        private BigDecimal totalEquity;
        private BigDecimal totalPositionValue;
        private BigDecimal totalUnrealizedPnl;
        private BigDecimal totalRealizedPnl;
        private BigDecimal marginUsed;
        private BigDecimal marginAvailable;
        private BigDecimal buyingPower;
        private List<Position> positions;
        private Instant timestamp;
    }

    @lombok.Data
    @lombok.Builder
    public static class PortfolioPerformance {
        private String userId;
        private Instant fromDate;
        private Instant toDate;
        private BigDecimal totalReturn;
        private BigDecimal totalReturnPercentage;
    }
}