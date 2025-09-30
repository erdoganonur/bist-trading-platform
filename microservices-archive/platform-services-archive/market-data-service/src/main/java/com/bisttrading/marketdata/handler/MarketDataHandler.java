package com.bisttrading.marketdata.handler;

import com.bisttrading.marketdata.dto.MarketMessage;
import com.bisttrading.marketdata.service.MarketDataPublisher;
import com.bisttrading.marketdata.service.MarketDataStorage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * MarketData mesajlarını işleyen handler
 * Python process_msg fonksiyonunun karşılığı
 */
@Slf4j
@Component
public class MarketDataHandler {

    private final MarketDataPublisher publisher;
    private final MarketDataStorage storage;
    private final ObjectMapper objectMapper;

    public MarketDataHandler(MarketDataPublisher publisher,
                           MarketDataStorage storage,
                           ObjectMapper objectMapper) {
        this.publisher = publisher;
        this.storage = storage;
        this.objectMapper = objectMapper;
    }

    /**
     * Gelen market data mesajını işler
     * Python: process_msg(msg) fonksiyonunun karşılığı
     */
    public void handleMessage(MarketMessage message) {
        try {
            String type = message.getType();
            String content = message.getContent();

            if (type == null || content == null) {
                log.warn("Eksik mesaj verisi: type={}, content={}", type, content);
                return;
            }

            log.debug("Mesaj işleniyor: Type={}, Content length={}", type, content.length());

            switch (type) {
                case "T" -> handleTickData(content);
                case "D" -> handleDepthData(content);
                case "O" -> handleOrderStatus(content);
                case "H" -> handleHeartbeat(content);
                default -> log.warn("Bilinmeyen mesaj tipi: {}", type);
            }

        } catch (Exception e) {
            log.error("Mesaj işleme hatası: {}", e.getMessage(), e);
        }
    }

    /**
     * Tick data mesajlarını işler (Type: "T")
     * Fiyat güncellemeleri ve işlem verileri
     */
    private void handleTickData(String content) {
        try {
            MarketMessage.TickData tickData = objectMapper.readValue(content, MarketMessage.TickData.class);

            if (tickData.getSymbol() == null) {
                log.warn("Tick data'da sembol bulunamadı: {}", content);
                return;
            }

            log.debug("Tick data alındı: {} - {} @ {}",
                tickData.getSymbol(),
                tickData.getVolume(),
                tickData.getPrice()
            );

            // Redis'e fiyat güncellemesi yayınla
            publisher.publishPriceUpdate(tickData);

            // TimescaleDB'ye trade bilgisini kaydet
            storage.saveMarketTick(tickData);

            // Trade execution verisi varsa ayrıca işle
            if (tickData.getDirection() != null) {
                publisher.publishTradeExecution(tickData);
            }

        } catch (JsonProcessingException e) {
            log.error("Tick data parse hatası: {}", e.getMessage());
            log.debug("Hatalı tick data content: {}", content);
        } catch (Exception e) {
            log.error("Tick data işleme hatası: {}", e.getMessage(), e);
        }
    }

    /**
     * Depth data mesajlarını işler (Type: "D")
     * Order book derinlik verileri
     */
    private void handleDepthData(String content) {
        try {
            MarketMessage.DepthData depthData = objectMapper.readValue(content, MarketMessage.DepthData.class);

            if (depthData.getSymbol() == null) {
                log.warn("Depth data'da sembol bulunamadı: {}", content);
                return;
            }

            log.debug("Depth data alındı: {} - Bids: {}, Asks: {}",
                depthData.getSymbol(),
                depthData.getBids() != null ? depthData.getBids().length : 0,
                depthData.getAsks() != null ? depthData.getAsks().length : 0
            );

            // Redis'e order book güncellemesi yayınla
            publisher.publishOrderBookUpdate(depthData);

            // TimescaleDB'ye order book snapshot'ı kaydet
            storage.saveOrderBookSnapshot(depthData);

        } catch (JsonProcessingException e) {
            log.error("Depth data parse hatası: {}", e.getMessage());
            log.debug("Hatalı depth data content: {}", content);
        } catch (Exception e) {
            log.error("Depth data işleme hatası: {}", e.getMessage(), e);
        }
    }

    /**
     * Order status mesajlarını işler (Type: "O")
     * Emir durumu güncellemeleri
     */
    private void handleOrderStatus(String content) {
        try {
            MarketMessage.OrderStatus orderStatus = objectMapper.readValue(content, MarketMessage.OrderStatus.class);

            if (orderStatus.getOrderId() == null) {
                log.warn("Order status'da order ID bulunamadı: {}", content);
                return;
            }

            log.debug("Order status alındı: {} - Status: {} ({})",
                orderStatus.getOrderId(),
                orderStatus.getStatus(),
                orderStatus.getStatusText()
            );

            // Redis'e order status güncellemesi yayınla
            publisher.publishOrderStatusUpdate(orderStatus);

            // TimescaleDB'ye order status'u kaydet
            storage.saveOrderStatus(orderStatus);

        } catch (JsonProcessingException e) {
            log.error("Order status parse hatası: {}", e.getMessage());
            log.debug("Hatalı order status content: {}", content);
        } catch (Exception e) {
            log.error("Order status işleme hatası: {}", e.getMessage(), e);
        }
    }

    /**
     * Heartbeat mesajlarını işler (Type: "H")
     * Bağlantı durumu kontrolü
     */
    private void handleHeartbeat(String content) {
        try {
            log.debug("Heartbeat alındı: {}", content);

            MarketMessage.Heartbeat heartbeat = objectMapper.readValue(content, MarketMessage.Heartbeat.class);

            // Redis'e heartbeat yayınla (sistem durumu için)
            publisher.publishHeartbeat(heartbeat);

        } catch (JsonProcessingException e) {
            log.error("Heartbeat parse hatası: {}", e.getMessage());
            log.debug("Hatalı heartbeat content: {}", content);
        } catch (Exception e) {
            log.error("Heartbeat işleme hatası: {}", e.getMessage(), e);
        }
    }
}