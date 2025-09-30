package com.bisttrading.marketdata.service;

import com.bisttrading.marketdata.dto.MarketMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

/**
 * Market data Redis publisher service
 * WebSocket'tan gelen verileri Redis channels'larına yayınlar
 */
@Slf4j
@Service
public class MarketDataPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // Redis channel patterns
    private final String pricesChannelPattern;
    private final String orderbookChannelPattern;
    private final String tradesChannelPattern;
    private final String heartbeatChannel;

    // TTL values
    private final Duration marketDataTtl;
    private final Duration orderbookTtl;

    public MarketDataPublisher(RedisTemplate<String, Object> redisTemplate,
                              ObjectMapper objectMapper,
                              @Value("${redis.channels.prices:prices:{symbol}}") String pricesChannelPattern,
                              @Value("${redis.channels.orderbook:orderbook:{symbol}}") String orderbookChannelPattern,
                              @Value("${redis.channels.trades:trades:{symbol}}") String tradesChannelPattern,
                              @Value("${redis.channels.heartbeat:marketdata:heartbeat}") String heartbeatChannel,
                              @Value("${redis.ttl.market-data:300s}") Duration marketDataTtl,
                              @Value("${redis.ttl.orderbook:60s}") Duration orderbookTtl) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.pricesChannelPattern = pricesChannelPattern;
        this.orderbookChannelPattern = orderbookChannelPattern;
        this.tradesChannelPattern = tradesChannelPattern;
        this.heartbeatChannel = heartbeatChannel;
        this.marketDataTtl = marketDataTtl;
        this.orderbookTtl = orderbookTtl;
    }

    /**
     * Fiyat güncellenmesi yayınlar
     * Channel: prices:{symbol}
     */
    public void publishPriceUpdate(MarketMessage.TickData tickData) {
        try {
            String symbol = tickData.getSymbol();
            String channel = pricesChannelPattern.replace("{symbol}", symbol);

            // Redis'e fiyat bilgisini kaydet (TTL ile)
            String priceKey = "price:" + symbol;
            Map<String, Object> priceData = Map.of(
                "symbol", symbol,
                "price", tickData.getPrice(),
                "volume", tickData.getVolume() != null ? tickData.getVolume() : 0,
                "timestamp", System.currentTimeMillis()
            );

            redisTemplate.opsForValue().set(priceKey, priceData, marketDataTtl);

            // Channel'a yayınla
            String message = objectMapper.writeValueAsString(priceData);
            redisTemplate.convertAndSend(channel, message);

            log.debug("Fiyat güncellenmesi yayınlandı: {} -> {}", symbol, tickData.getPrice());

        } catch (JsonProcessingException e) {
            log.error("Fiyat güncellenmesi JSON serialize hatası: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Fiyat güncellenmesi yayın hatası: {}", e.getMessage(), e);
        }
    }

    /**
     * Order book güncellenmesi yayınlar
     * Channel: orderbook:{symbol}
     */
    public void publishOrderBookUpdate(MarketMessage.DepthData depthData) {
        try {
            String symbol = depthData.getSymbol();
            String channel = orderbookChannelPattern.replace("{symbol}", symbol);

            // Order book verilerini hazırla
            Map<String, Object> orderbookData = Map.of(
                "symbol", symbol,
                "bids", depthData.getBids() != null ? depthData.getBids() : new MarketMessage.BookEntry[0],
                "asks", depthData.getAsks() != null ? depthData.getAsks() : new MarketMessage.BookEntry[0],
                "timestamp", System.currentTimeMillis()
            );

            // Redis'e order book snapshot'ı kaydet (TTL ile)
            String orderbookKey = "orderbook:" + symbol;
            redisTemplate.opsForValue().set(orderbookKey, orderbookData, orderbookTtl);

            // Channel'a yayınla
            String message = objectMapper.writeValueAsString(orderbookData);
            redisTemplate.convertAndSend(channel, message);

            log.debug("Order book güncellenmesi yayınlandı: {} -> Bids: {}, Asks: {}",
                symbol,
                depthData.getBids() != null ? depthData.getBids().length : 0,
                depthData.getAsks() != null ? depthData.getAsks().length : 0
            );

        } catch (JsonProcessingException e) {
            log.error("Order book güncellenmesi JSON serialize hatası: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Order book güncellenmesi yayın hatası: {}", e.getMessage(), e);
        }
    }

    /**
     * Trade execution yayınlar
     * Channel: trades:{symbol}
     */
    public void publishTradeExecution(MarketMessage.TickData tickData) {
        try {
            String symbol = tickData.getSymbol();
            String channel = tradesChannelPattern.replace("{symbol}", symbol);

            Map<String, Object> tradeData = Map.of(
                "symbol", symbol,
                "price", tickData.getPrice(),
                "volume", tickData.getVolume() != null ? tickData.getVolume() : 0,
                "direction", tickData.getDirection() != null ? tickData.getDirection() : "UNKNOWN",
                "value", tickData.getValue() != null ? tickData.getValue() : tickData.getPrice(),
                "buyer", tickData.getBuyer() != null ? tickData.getBuyer() : "",
                "seller", tickData.getSeller() != null ? tickData.getSeller() : "",
                "time", tickData.getTime() != null ? tickData.getTime() : "",
                "timestamp", System.currentTimeMillis()
            );

            // Son trade bilgisini kaydet
            String lastTradeKey = "last_trade:" + symbol;
            redisTemplate.opsForValue().set(lastTradeKey, tradeData, marketDataTtl);

            // Trade history listesine ekle (son 100 trade)
            String tradeHistoryKey = "trade_history:" + symbol;
            redisTemplate.opsForList().leftPush(tradeHistoryKey, tradeData);
            redisTemplate.opsForList().trim(tradeHistoryKey, 0, 99); // Son 100 trade tut
            redisTemplate.expire(tradeHistoryKey, marketDataTtl);

            // Channel'a yayınla
            String message = objectMapper.writeValueAsString(tradeData);
            redisTemplate.convertAndSend(channel, message);

            log.debug("Trade execution yayınlandı: {} - {} {} @ {}",
                symbol, tickData.getDirection(), tickData.getVolume(), tickData.getPrice());

        } catch (JsonProcessingException e) {
            log.error("Trade execution JSON serialize hatası: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Trade execution yayın hatası: {}", e.getMessage(), e);
        }
    }

    /**
     * Order status güncellenmesi yayınlar
     * Channel: order_status (global)
     */
    public void publishOrderStatusUpdate(MarketMessage.OrderStatus orderStatus) {
        try {
            Map<String, Object> statusData = Map.of(
                "orderId", orderStatus.getOrderId(),
                "symbol", orderStatus.getSymbol() != null ? orderStatus.getSymbol() : "",
                "status", orderStatus.getStatus() != null ? orderStatus.getStatus() : 0,
                "statusText", orderStatus.getStatusText() != null ? orderStatus.getStatusText() : "",
                "quantity", orderStatus.getQuantity() != null ? orderStatus.getQuantity() : 0,
                "filledQuantity", orderStatus.getFilledQuantity() != null ? orderStatus.getFilledQuantity() : 0,
                "price", orderStatus.getPrice() != null ? orderStatus.getPrice() : 0,
                "timestamp", System.currentTimeMillis()
            );

            // Order status'u Redis'e kaydet
            String orderKey = "order_status:" + orderStatus.getOrderId();
            redisTemplate.opsForValue().set(orderKey, statusData, marketDataTtl);

            // Channel'a yayınla
            String message = objectMapper.writeValueAsString(statusData);
            redisTemplate.convertAndSend("order_status", message);

            log.debug("Order status yayınlandı: {} - Status: {} ({})",
                orderStatus.getOrderId(), orderStatus.getStatus(), orderStatus.getStatusText());

        } catch (JsonProcessingException e) {
            log.error("Order status JSON serialize hatası: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Order status yayın hatası: {}", e.getMessage(), e);
        }
    }

    /**
     * Heartbeat yayınlar (sistem durumu için)
     */
    public void publishHeartbeat(MarketMessage.Heartbeat heartbeat) {
        try {
            Map<String, Object> heartbeatData = Map.of(
                "status", heartbeat.getStatus() != null ? heartbeat.getStatus() : "alive",
                "timestamp", System.currentTimeMillis(),
                "service", "market-data-service"
            );

            // Heartbeat bilgisini Redis'e kaydet (kısa TTL)
            redisTemplate.opsForValue().set("marketdata:heartbeat", heartbeatData, Duration.ofSeconds(60));

            // Channel'a yayınla
            String message = objectMapper.writeValueAsString(heartbeatData);
            redisTemplate.convertAndSend(heartbeatChannel, message);

            log.trace("Heartbeat yayınlandı");

        } catch (JsonProcessingException e) {
            log.error("Heartbeat JSON serialize hatası: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Heartbeat yayın hatası: {}", e.getMessage(), e);
        }
    }

    /**
     * Sembol için mevcut fiyat bilgisini Redis'ten alır
     */
    public Object getCurrentPrice(String symbol) {
        try {
            String priceKey = "price:" + symbol;
            return redisTemplate.opsForValue().get(priceKey);
        } catch (Exception e) {
            log.error("Fiyat bilgisi alma hatası: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Sembol için mevcut order book'u Redis'ten alır
     */
    public Object getCurrentOrderBook(String symbol) {
        try {
            String orderbookKey = "orderbook:" + symbol;
            return redisTemplate.opsForValue().get(orderbookKey);
        } catch (Exception e) {
            log.error("Order book bilgisi alma hatası: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Sembol için son trade'leri Redis'ten alır
     */
    public Object getRecentTrades(String symbol, long limit) {
        try {
            String tradeHistoryKey = "trade_history:" + symbol;
            return redisTemplate.opsForList().range(tradeHistoryKey, 0, limit - 1);
        } catch (Exception e) {
            log.error("Son trade'ler alma hatası: {}", e.getMessage());
            return null;
        }
    }
}