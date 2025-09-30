package com.bisttrading.marketdata.service;

import com.bisttrading.marketdata.dto.MarketMessage;
import com.bisttrading.marketdata.entity.MarketTick;
import com.bisttrading.marketdata.entity.OrderBookSnapshot;
import com.bisttrading.marketdata.entity.OrderStatusHistory;
import com.bisttrading.marketdata.repository.MarketTickRepository;
import com.bisttrading.marketdata.repository.OrderBookSnapshotRepository;
import com.bisttrading.marketdata.repository.OrderStatusHistoryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Market data TimescaleDB storage service
 * WebSocket'tan gelen verileri TimescaleDB'ye batch olarak kaydeder
 */
@Slf4j
@Service
public class MarketDataStorage {

    private final MarketTickRepository marketTickRepository;
    private final OrderBookSnapshotRepository orderBookRepository;
    private final OrderStatusHistoryRepository orderStatusRepository;
    private final ObjectMapper objectMapper;

    // Batch configuration
    private final int batchSize;

    public MarketDataStorage(MarketTickRepository marketTickRepository,
                           OrderBookSnapshotRepository orderBookRepository,
                           OrderStatusHistoryRepository orderStatusRepository,
                           ObjectMapper objectMapper,
                           @Value("${timescaledb.batch.size:1000}") int batchSize) {
        this.marketTickRepository = marketTickRepository;
        this.orderBookRepository = orderBookRepository;
        this.orderStatusRepository = orderStatusRepository;
        this.objectMapper = objectMapper;
        this.batchSize = batchSize;
    }

    /**
     * Market tick verisini TimescaleDB'ye asenkron olarak kaydeder
     */
    @Async
    @Transactional
    public CompletableFuture<Void> saveMarketTick(MarketMessage.TickData tickData) {
        try {
            if (tickData.getSymbol() == null || tickData.getPrice() == null) {
                log.warn("Geçersiz tick data, kayıt atlanıyor: {}", tickData);
                return CompletableFuture.completedFuture(null);
            }

            MarketTick marketTick = MarketTick.builder()
                .symbol(tickData.getSymbol())
                .price(tickData.getPrice())
                .quantity(tickData.getVolume() != null ? tickData.getVolume() : 0L)
                .direction(tickData.getDirection())
                .value(tickData.getValue())
                .buyer(tickData.getBuyer())
                .seller(tickData.getSeller())
                .tradeTime(tickData.getTime());

            marketTickRepository.save(marketTick);

            log.trace("Market tick kaydedildi: {} - {} @ {}",
                tickData.getSymbol(), tickData.getVolume(), tickData.getPrice());

        } catch (Exception e) {
            log.error("Market tick kayıt hatası: {}", e.getMessage(), e);
            throw e;
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Order book snapshot'ını TimescaleDB'ye asenkron olarak kaydeder
     */
    @Async
    @Transactional
    public CompletableFuture<Void> saveOrderBookSnapshot(MarketMessage.DepthData depthData) {
        try {
            if (depthData.getSymbol() == null) {
                log.warn("Geçersiz depth data, kayıt atlanıyor: {}", depthData);
                return CompletableFuture.completedFuture(null);
            }

            String bidsJson = null;
            String asksJson = null;
            int bidCount = 0;
            int askCount = 0;

            // Bid ve Ask verilerini JSON'a serialize et
            if (depthData.getBids() != null && depthData.getBids().length > 0) {
                bidsJson = objectMapper.writeValueAsString(depthData.getBids());
                bidCount = depthData.getBids().length;
            }

            if (depthData.getAsks() != null && depthData.getAsks().length > 0) {
                asksJson = objectMapper.writeValueAsString(depthData.getAsks());
                askCount = depthData.getAsks().length;
            }

            OrderBookSnapshot snapshot = OrderBookSnapshot.builder()
                .symbol(depthData.getSymbol())
                .bids(bidsJson)
                .asks(asksJson)
                .bidCount(bidCount)
                .askCount(askCount);

            orderBookRepository.save(snapshot);

            log.trace("Order book snapshot kaydedildi: {} - Bids: {}, Asks: {}",
                depthData.getSymbol(), bidCount, askCount);

        } catch (JsonProcessingException e) {
            log.error("Order book JSON serialize hatası: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Order book snapshot kayıt hatası: {}", e.getMessage(), e);
            throw e;
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Order status'u TimescaleDB'ye asenkron olarak kaydeder
     */
    @Async
    @Transactional
    public CompletableFuture<Void> saveOrderStatus(MarketMessage.OrderStatus orderStatus) {
        try {
            if (orderStatus.getOrderId() == null) {
                log.warn("Geçersiz order status, kayıt atlanıyor: {}", orderStatus);
                return CompletableFuture.completedFuture(null);
            }

            OrderStatusHistory statusHistory = OrderStatusHistory.builder()
                .orderId(orderStatus.getOrderId())
                .symbol(orderStatus.getSymbol())
                .status(orderStatus.getStatus())
                .statusText(orderStatus.getStatusText())
                .quantity(orderStatus.getQuantity())
                .filledQuantity(orderStatus.getFilledQuantity())
                .price(orderStatus.getPrice())
;

            orderStatusRepository.save(statusHistory);

            log.trace("Order status kaydedildi: {} - Status: {} ({})",
                orderStatus.getOrderId(), orderStatus.getStatus(), orderStatus.getStatusText());

        } catch (Exception e) {
            log.error("Order status kayıt hatası: {}", e.getMessage(), e);
            throw e;
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Batch market tick kaydetme
     */
    @Async
    @Transactional
    public CompletableFuture<Void> saveMarketTicksBatch(List<MarketMessage.TickData> tickDataList) {
        try {
            List<MarketTick> marketTicks = tickDataList.stream()
                .filter(tick -> tick.getSymbol() != null && tick.getPrice() != null)
                .map(tick -> MarketTick.builder()
                    .symbol(tick.getSymbol())
                    .price(tick.getPrice())
                    .quantity(tick.getVolume() != null ? tick.getVolume() : 0L)
                    .direction(tick.getDirection())
                    .value(tick.getValue())
                    .buyer(tick.getBuyer())
                    .seller(tick.getSeller())
                    .tradeTime(tick.getTime())
                    )
                .collect(java.util.stream.Collectors.toList());

            if (!marketTicks.isEmpty()) {
                marketTickRepository.saveAll(marketTicks);
                log.debug("Batch market tick kaydedildi: {} kayıt", marketTicks.size());
            }

        } catch (Exception e) {
            log.error("Batch market tick kayıt hatası: {}", e.getMessage(), e);
            throw e;
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Batch order book snapshot kaydetme
     */
    @Async
    @Transactional
    public CompletableFuture<Void> saveOrderBookSnapshotsBatch(List<MarketMessage.DepthData> depthDataList) {
        try {
            List<OrderBookSnapshot> snapshots = depthDataList.stream()
                .filter(depth -> depth.getSymbol() != null)
                .map(depth -> {
                    try {
                        String bidsJson = null;
                        String asksJson = null;
                        int bidCount = 0;
                        int askCount = 0;

                        if (depth.getBids() != null && depth.getBids().length > 0) {
                            bidsJson = objectMapper.writeValueAsString(depth.getBids());
                            bidCount = depth.getBids().length;
                        }

                        if (depth.getAsks() != null && depth.getAsks().length > 0) {
                            asksJson = objectMapper.writeValueAsString(depth.getAsks());
                            askCount = depth.getAsks().length;
                        }

                        return OrderBookSnapshot.builder()
                            .symbol(depth.getSymbol())
                            .bids(bidsJson)
                            .asks(asksJson)
                            .bidCount(bidCount)
                            .askCount(askCount)
            ;

                    } catch (JsonProcessingException e) {
                        log.error("Order book JSON serialize hatası: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(snapshot -> snapshot != null)
                .collect(java.util.stream.Collectors.toList());

            if (!snapshots.isEmpty()) {
                orderBookRepository.saveAll(snapshots);
                log.debug("Batch order book snapshot kaydedildi: {} kayıt", snapshots.size());
            }

        } catch (Exception e) {
            log.error("Batch order book snapshot kayıt hatası: {}", e.getMessage(), e);
            throw e;
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Data retention - eski verileri temizleme
     */
    @Transactional
    public void cleanupOldData(int retentionDays) {
        try {
            OffsetDateTime cutoffTime = OffsetDateTime.now().minusDays(retentionDays);

            // Market tick verilerini temizle
            int deletedTicks = marketTickRepository.deleteOldData(cutoffTime);
            log.info("Market tick temizleme: {} kayıt silindi", deletedTicks);

            // Order book snapshot'larını temizle
            int deletedSnapshots = orderBookRepository.deleteOldSnapshots(cutoffTime);
            log.info("Order book snapshot temizleme: {} kayıt silindi", deletedSnapshots);

            // Order status verilerini temizle
            int deletedStatuses = orderStatusRepository.deleteOldOrderStatuses(cutoffTime.toLocalDateTime());
            log.info("Order status temizleme: {} kayıt silindi", deletedStatuses);

        } catch (Exception e) {
            log.error("Data cleanup hatası: {}", e.getMessage(), e);
        }
    }

    /**
     * Database istatistiklerini alır
     */
    public DatabaseStats getDatabaseStats() {
        try {
            OffsetDateTime startOfDay = OffsetDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            OffsetDateTime endOfDay = startOfDay.plusDays(1);

            // Aktif semboller
            List<String> activeSymbols = marketTickRepository.findActiveSymbolsSince(OffsetDateTime.now().minusHours(24));

            return DatabaseStats.builder()
                .activeSymbols(activeSymbols.size())
                .symbolList(activeSymbols)
                .totalTicksToday(0L) // TODO: Implement
                .totalSnapshotsToday(0L) // TODO: Implement
                .totalOrderStatusesToday(0L) // TODO: Implement
                .build();

        } catch (Exception e) {
            log.error("Database stats alma hatası: {}", e.getMessage(), e);
            return DatabaseStats.builder().build();
        }
    }

    /**
     * Database istatistikleri DTO
     */
    @lombok.Builder
    @lombok.Data
    public static class DatabaseStats {
        private int activeSymbols;
        private List<String> symbolList;
        private long totalTicksToday;
        private long totalSnapshotsToday;
        private long totalOrderStatusesToday;

    }
}