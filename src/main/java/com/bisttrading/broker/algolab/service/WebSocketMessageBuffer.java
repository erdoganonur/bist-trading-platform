package com.bisttrading.broker.algolab.service;

import com.bisttrading.broker.algolab.dto.websocket.TickData;
import com.bisttrading.broker.algolab.dto.websocket.OrderBookData;
import com.bisttrading.broker.algolab.dto.websocket.TradeData;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Buffers WebSocket messages for HTTP polling access.
 * Stores recent messages in memory for CLI client consumption.
 */
@Service
@Slf4j
public class WebSocketMessageBuffer {

    private static final int MAX_BUFFER_SIZE = 100;
    private static final long MAX_MESSAGE_AGE_MS = 60000; // 1 minute

    private final Map<String, ConcurrentLinkedQueue<BufferedTickMessage>> tickBuffer = new ConcurrentHashMap<>();
    private final Map<String, ConcurrentLinkedQueue<BufferedOrderBookMessage>> orderBookBuffer = new ConcurrentHashMap<>();
    private final Map<String, ConcurrentLinkedQueue<BufferedTradeMessage>> tradeBuffer = new ConcurrentHashMap<>();

    /**
     * Add tick data to buffer.
     */
    public void addTick(String symbol, TickData data) {
        tickBuffer.computeIfAbsent(symbol, k -> new ConcurrentLinkedQueue<>())
            .offer(new BufferedTickMessage(data, Instant.now()));

        // Trim buffer if too large
        trimBuffer(tickBuffer.get(symbol), MAX_BUFFER_SIZE);

        log.trace("Buffered tick for {}: Price={}", symbol, data.getLastPrice());
    }

    /**
     * Add order book data to buffer.
     */
    public void addOrderBook(String symbol, OrderBookData data) {
        orderBookBuffer.computeIfAbsent(symbol, k -> new ConcurrentLinkedQueue<>())
            .offer(new BufferedOrderBookMessage(data, Instant.now()));

        trimBuffer(orderBookBuffer.get(symbol), MAX_BUFFER_SIZE);

        log.trace("Buffered order book for {}", symbol);
    }

    /**
     * Add trade data to buffer.
     */
    public void addTrade(String symbol, TradeData data) {
        tradeBuffer.computeIfAbsent(symbol, k -> new ConcurrentLinkedQueue<>())
            .offer(new BufferedTradeMessage(data, Instant.now()));

        trimBuffer(tradeBuffer.get(symbol), MAX_BUFFER_SIZE);

        log.trace("Buffered trade for {}", symbol);
    }

    /**
     * Get recent tick messages for a symbol.
     */
    public List<BufferedTickMessage> getRecentTicks(String symbol, int limit) {
        var buffer = tickBuffer.get(symbol);
        if (buffer == null) {
            return Collections.emptyList();
        }

        cleanOldMessages(buffer);

        return buffer.stream()
            .skip(Math.max(0, buffer.size() - limit))
            .toList();
    }

    /**
     * Get recent order book messages for a symbol.
     */
    public List<BufferedOrderBookMessage> getRecentOrderBooks(String symbol, int limit) {
        var buffer = orderBookBuffer.get(symbol);
        if (buffer == null) {
            return Collections.emptyList();
        }

        cleanOldMessages(buffer);

        return buffer.stream()
            .skip(Math.max(0, buffer.size() - limit))
            .toList();
    }

    /**
     * Get recent trade messages for a symbol.
     */
    public List<BufferedTradeMessage> getRecentTrades(String symbol, int limit) {
        var buffer = tradeBuffer.get(symbol);
        if (buffer == null) {
            return Collections.emptyList();
        }

        cleanOldMessages(buffer);

        return buffer.stream()
            .skip(Math.max(0, buffer.size() - limit))
            .toList();
    }

    /**
     * Get all symbols with buffered tick data.
     */
    public Set<String> getActiveTickSymbols() {
        return new HashSet<>(tickBuffer.keySet());
    }

    /**
     * Clear all buffers.
     */
    public void clearAll() {
        tickBuffer.clear();
        orderBookBuffer.clear();
        tradeBuffer.clear();
        log.info("All message buffers cleared");
    }

    /**
     * Clear buffer for specific symbol.
     */
    public void clearSymbol(String symbol) {
        tickBuffer.remove(symbol);
        orderBookBuffer.remove(symbol);
        tradeBuffer.remove(symbol);
        log.info("Cleared buffers for symbol: {}", symbol);
    }

    /**
     * Get buffer statistics.
     */
    public Map<String, Object> getStats() {
        return Map.of(
            "tickSymbols", tickBuffer.size(),
            "orderBookSymbols", orderBookBuffer.size(),
            "tradeSymbols", tradeBuffer.size(),
            "totalTickMessages", tickBuffer.values().stream().mapToInt(Queue::size).sum(),
            "totalOrderBookMessages", orderBookBuffer.values().stream().mapToInt(Queue::size).sum(),
            "totalTradeMessages", tradeBuffer.values().stream().mapToInt(Queue::size).sum()
        );
    }

    private <T> void trimBuffer(Queue<T> buffer, int maxSize) {
        while (buffer.size() > maxSize) {
            buffer.poll();
        }
    }

    private <T extends BufferedMessage> void cleanOldMessages(Queue<T> buffer) {
        Instant cutoff = Instant.now().minusMillis(MAX_MESSAGE_AGE_MS);
        buffer.removeIf(msg -> msg.getReceivedAt().isBefore(cutoff));
    }

    // Buffered message wrappers

    @Data
    public static class BufferedTickMessage implements BufferedMessage {
        private final TickData data;
        private final Instant receivedAt;
    }

    @Data
    public static class BufferedOrderBookMessage implements BufferedMessage {
        private final OrderBookData data;
        private final Instant receivedAt;
    }

    @Data
    public static class BufferedTradeMessage implements BufferedMessage {
        private final TradeData data;
        private final Instant receivedAt;
    }

    private interface BufferedMessage {
        Instant getReceivedAt();
    }
}
