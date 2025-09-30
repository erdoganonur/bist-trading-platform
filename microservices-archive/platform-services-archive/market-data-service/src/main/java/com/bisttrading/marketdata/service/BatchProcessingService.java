package com.bisttrading.marketdata.service;

import com.bisttrading.marketdata.dto.MarketMessage;
import com.bisttrading.marketdata.entity.MarketTick;
import com.bisttrading.marketdata.entity.OrderBookSnapshot;
import com.bisttrading.marketdata.repository.MarketTickRepository;
import com.bisttrading.marketdata.repository.OrderBookSnapshotRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * High-performance batch processing service for market data ingestion
 * Optimized for TimescaleDB bulk operations
 */
@Slf4j
@Service
public class BatchProcessingService {

    private final MarketTickRepository marketTickRepository;
    private final OrderBookSnapshotRepository orderBookRepository;

    // Batch configuration
    private final int tickBatchSize;
    private final int orderBookBatchSize;
    private final long flushIntervalMs;

    // In-memory buffers for batching
    private final ConcurrentLinkedQueue<MarketMessage.TickData> tickBuffer = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<MarketMessage.DepthData> orderBookBuffer = new ConcurrentLinkedQueue<>();

    // Performance metrics
    private final AtomicLong totalTicksProcessed = new AtomicLong(0);
    private final AtomicLong totalOrderBooksProcessed = new AtomicLong(0);
    private final AtomicLong totalBatchesProcessed = new AtomicLong(0);
    private final Map<String, Long> symbolTickCounts = new ConcurrentHashMap<>();

    // Buffer size limits
    private final int maxBufferSize;

    public BatchProcessingService(MarketTickRepository marketTickRepository,
                                OrderBookSnapshotRepository orderBookRepository,
                                @Value("${timescaledb.batch.tick-size:1000}") int tickBatchSize,
                                @Value("${timescaledb.batch.orderbook-size:100}") int orderBookBatchSize,
                                @Value("${timescaledb.batch.flush-interval:5000}") long flushIntervalMs,
                                @Value("${timescaledb.batch.max-buffer-size:10000}") int maxBufferSize) {
        this.marketTickRepository = marketTickRepository;
        this.orderBookRepository = orderBookRepository;
        this.tickBatchSize = tickBatchSize;
        this.orderBookBatchSize = orderBookBatchSize;
        this.flushIntervalMs = flushIntervalMs;
        this.maxBufferSize = maxBufferSize;
    }

    // =====================================
    // TICK DATA BATCH PROCESSING
    // =====================================

    /**
     * Tick data'yı buffer'a ekler
     */
    public void addTickData(MarketMessage.TickData tickData) {
        if (tickData == null || tickData.getSymbol() == null) {
            log.warn("Geçersiz tick data, buffer'a eklenmedi");
            return;
        }

        // Buffer overflow koruması
        if (tickBuffer.size() >= maxBufferSize) {
            log.warn("Tick buffer overflow! Zorla flush yapılıyor...");
            flushTickBuffer();
        }

        tickBuffer.offer(tickData);
        symbolTickCounts.merge(tickData.getSymbol(), 1L, Long::sum);

        // Batch size'a ulaştıysak flush et
        if (tickBuffer.size() >= tickBatchSize) {
            flushTickBuffer();
        }
    }

    /**
     * Tick buffer'ı flush eder
     */
    @Async
    @Transactional
    public CompletableFuture<Void> flushTickBuffer() {
        List<MarketMessage.TickData> batch = new ArrayList<>();

        // Buffer'dan batch size kadar veri al
        MarketMessage.TickData tick;
        while (batch.size() < tickBatchSize && (tick = tickBuffer.poll()) != null) {
            batch.add(tick);
        }

        if (batch.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        try {
            long startTime = System.currentTimeMillis();

            // Batch native insert kullanarak high-performance insertion
            processBatchTickInsert(batch);

            long processingTime = System.currentTimeMillis() - startTime;
            totalTicksProcessed.addAndGet(batch.size());
            totalBatchesProcessed.incrementAndGet();

            log.debug("Tick batch processed: {} records in {}ms (Buffer remaining: {})",
                batch.size(), processingTime, tickBuffer.size());

            // Performance monitoring
            if (processingTime > 1000) { // 1 second threshold
                log.warn("Yavaş tick batch processing: {}ms for {} records", processingTime, batch.size());
            }

        } catch (Exception e) {
            log.error("Tick batch processing hatası: {}", e.getMessage(), e);
            // Re-add failed items back to buffer for retry
            batch.forEach(tickBuffer::offer);
            throw e;
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Batch tick insert implementation
     */
    private void processBatchTickInsert(List<MarketMessage.TickData> batch) {
        OffsetDateTime now = OffsetDateTime.now();

        // Use repository batch methods
        batch.parallelStream().forEach(tick -> {
            try {
                marketTickRepository.insertTickWithConflictIgnore(
                    now,
                    tick.getSymbol(),
                    tick.getPrice(),
                    tick.getVolume() != null ? tick.getVolume() : 0L,
                    null, // bid
                    null, // ask
                    null, // bidSize
                    null, // askSize
                    tick.getVolume() != null ? tick.getVolume() : 0L,
                    1L, // trades
                    tick.getPrice(), // vwap
                    tick.getDirection(),
                    tick.getValue() != null ? tick.getValue() : tick.getPrice(),
                    tick.getBuyer(),
                    tick.getSeller(),
                    tick.getTime()
                );
            } catch (Exception e) {
                log.error("Individual tick insert failed: {}", e.getMessage());
            }
        });
    }

    // =====================================
    // ORDER BOOK BATCH PROCESSING
    // =====================================

    /**
     * Order book data'yı buffer'a ekler
     */
    public void addOrderBookData(MarketMessage.DepthData depthData) {
        if (depthData == null || depthData.getSymbol() == null) {
            log.warn("Geçersiz order book data, buffer'a eklenmedi");
            return;
        }

        // Buffer overflow koruması
        if (orderBookBuffer.size() >= maxBufferSize) {
            log.warn("Order book buffer overflow! Zorla flush yapılıyor...");
            flushOrderBookBuffer();
        }

        orderBookBuffer.offer(depthData);

        // Batch size'a ulaştıysak flush et
        if (orderBookBuffer.size() >= orderBookBatchSize) {
            flushOrderBookBuffer();
        }
    }

    /**
     * Order book buffer'ı flush eder
     */
    @Async
    @Transactional
    public CompletableFuture<Void> flushOrderBookBuffer() {
        List<MarketMessage.DepthData> batch = new ArrayList<>();

        // Buffer'dan batch size kadar veri al
        MarketMessage.DepthData depthData;
        while (batch.size() < orderBookBatchSize && (depthData = orderBookBuffer.poll()) != null) {
            batch.add(depthData);
        }

        if (batch.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        try {
            long startTime = System.currentTimeMillis();

            // Batch order book insert
            processBatchOrderBookInsert(batch);

            long processingTime = System.currentTimeMillis() - startTime;
            totalOrderBooksProcessed.addAndGet(batch.size());

            log.debug("Order book batch processed: {} records in {}ms (Buffer remaining: {})",
                batch.size(), processingTime, orderBookBuffer.size());

        } catch (Exception e) {
            log.error("Order book batch processing hatası: {}", e.getMessage(), e);
            // Re-add failed items back to buffer
            batch.forEach(orderBookBuffer::offer);
            throw e;
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Batch order book insert implementation
     */
    private void processBatchOrderBookInsert(List<MarketMessage.DepthData> batch) {
        OffsetDateTime now = OffsetDateTime.now();

        batch.parallelStream().forEach(depth -> {
            try {
                // Calculate metrics for each order book
                String bidsJson = depth.getBids() != null ?
                    new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(depth.getBids()) : null;
                String asksJson = depth.getAsks() != null ?
                    new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(depth.getAsks()) : null;

                int bidCount = depth.getBids() != null ? depth.getBids().length : 0;
                int askCount = depth.getAsks() != null ? depth.getAsks().length : 0;

                // Calculate best bid/ask
                BigDecimal bestBid = null;
                BigDecimal bestAsk = null;
                Long bestBidSize = null;
                Long bestAskSize = null;
                long totalBidVolume = 0;
                long totalAskVolume = 0;

                if (depth.getBids() != null && depth.getBids().length > 0) {
                    bestBid = depth.getBids()[0].getPrice();
                    bestBidSize = depth.getBids()[0].getQuantity();
                    for (var bid : depth.getBids()) {
                        totalBidVolume += bid.getQuantity() != null ? bid.getQuantity() : 0L;
                    }
                }

                if (depth.getAsks() != null && depth.getAsks().length > 0) {
                    bestAsk = depth.getAsks()[0].getPrice();
                    bestAskSize = depth.getAsks()[0].getQuantity();
                    for (var ask : depth.getAsks()) {
                        totalAskVolume += ask.getQuantity() != null ? ask.getQuantity() : 0L;
                    }
                }

                // Calculate spread and other metrics
                BigDecimal bidAskSpread = null;
                BigDecimal midPrice = null;
                BigDecimal imbalanceRatio = null;
                BigDecimal spreadPercentage = null;

                if (bestBid != null && bestAsk != null) {
                    bidAskSpread = bestAsk.subtract(bestBid);
                    midPrice = bestBid.add(bestAsk).divide(BigDecimal.valueOf(2));
                    if (midPrice.compareTo(BigDecimal.ZERO) > 0) {
                        spreadPercentage = bidAskSpread.divide(midPrice, 8, BigDecimal.ROUND_HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                    }
                }

                if (totalBidVolume > 0 || totalAskVolume > 0) {
                    long totalVolume = totalBidVolume + totalAskVolume;
                    imbalanceRatio = BigDecimal.valueOf(totalBidVolume)
                        .divide(BigDecimal.valueOf(totalVolume), 8, BigDecimal.ROUND_HALF_UP);
                }

                orderBookRepository.upsertOrderBookSnapshot(
                    now,
                    depth.getSymbol(),
                    bidsJson,
                    asksJson,
                    bidCount,
                    askCount,
                    totalBidVolume,
                    totalAskVolume,
                    bestBid,
                    bestAsk,
                    bestBidSize,
                    bestAskSize,
                    bidAskSpread,
                    midPrice,
                    imbalanceRatio,
                    spreadPercentage,
                    System.currentTimeMillis()
                );

            } catch (Exception e) {
                log.error("Individual order book insert failed for {}: {}",
                    depth.getSymbol(), e.getMessage());
            }
        });
    }

    // =====================================
    // SCHEDULED FLUSH OPERATIONS
    // =====================================

    /**
     * Periyodik buffer flush (configurable interval)
     */
    @Scheduled(fixedRateString = "${timescaledb.batch.flush-interval:5000}")
    public void scheduledFlush() {
        try {
            // Tick buffer flush
            if (!tickBuffer.isEmpty()) {
                flushTickBuffer();
            }

            // Order book buffer flush
            if (!orderBookBuffer.isEmpty()) {
                flushOrderBookBuffer();
            }

            // Performance reporting
            if (totalBatchesProcessed.get() % 100 == 0) { // Her 100 batch'te bir report
                reportPerformanceMetrics();
            }

        } catch (Exception e) {
            log.error("Scheduled flush hatası: {}", e.getMessage(), e);
        }
    }

    /**
     * Shutdown sırasında tüm buffer'ları flush et
     */
    @jakarta.annotation.PreDestroy
    public void flushAllBuffers() {
        log.info("Shutting down batch processing service, flushing all buffers...");

        try {
            // Son kez tüm buffer'ları flush et
            flushTickBuffer().get(); // Block until complete
            flushOrderBookBuffer().get(); // Block until complete

            log.info("All buffers flushed successfully on shutdown");
        } catch (Exception e) {
            log.error("Error flushing buffers on shutdown: {}", e.getMessage(), e);
        }
    }

    // =====================================
    // MONITORING & METRICS
    // =====================================

    /**
     * Performance metrics raporu
     */
    private void reportPerformanceMetrics() {
        long totalTicks = totalTicksProcessed.get();
        long totalOrderBooks = totalOrderBooksProcessed.get();
        long totalBatches = totalBatchesProcessed.get();

        log.info("Batch Processing Metrics - Ticks: {}, OrderBooks: {}, Batches: {}, " +
                "Buffer sizes - Ticks: {}, OrderBooks: {}",
            totalTicks, totalOrderBooks, totalBatches,
            tickBuffer.size(), orderBookBuffer.size());

        // Top symbols by tick count
        symbolTickCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .forEach(entry -> log.debug("Symbol {} processed {} ticks",
                entry.getKey(), entry.getValue()));
    }

    /**
     * Buffer durumlarını döndürür
     */
    public BatchProcessingMetrics getMetrics() {
        return BatchProcessingMetrics.builder()
            .tickBufferSize(tickBuffer.size())
            .orderBookBufferSize(orderBookBuffer.size())
            .totalTicksProcessed(totalTicksProcessed.get())
            .totalOrderBooksProcessed(totalOrderBooksProcessed.get())
            .totalBatchesProcessed(totalBatchesProcessed.get())
            .symbolTickCounts(new ConcurrentHashMap<>(symbolTickCounts))
            .maxBufferSize(maxBufferSize)
            .tickBatchSize(tickBatchSize)
            .orderBookBatchSize(orderBookBatchSize)
            .build();
    }

    /**
     * Buffer'ları manuel flush etme
     */
    public void forceFlush() {
        log.info("Manual flush triggered");
        flushTickBuffer();
        flushOrderBookBuffer();
    }

    /**
     * Metrics DTO
     */
    @lombok.Builder
    @lombok.Data
    public static class BatchProcessingMetrics {
        private int tickBufferSize;
        private int orderBookBufferSize;
        private long totalTicksProcessed;
        private long totalOrderBooksProcessed;
        private long totalBatchesProcessed;
        private Map<String, Long> symbolTickCounts;
        private int maxBufferSize;
        private int tickBatchSize;
        private int orderBookBatchSize;

        // Derived metrics
        public int getPendingTicks() { return tickBufferSize; }
        public int getPendingOrderBooks() { return orderBookBufferSize; }
        public int getActiveSymbolsCount() { return symbolTickCounts != null ? symbolTickCounts.size() : 0; }
    }
}