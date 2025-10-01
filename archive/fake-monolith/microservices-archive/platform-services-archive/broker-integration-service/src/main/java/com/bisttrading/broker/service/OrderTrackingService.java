package com.bisttrading.broker.service;

import com.bisttrading.broker.algolab.model.OrderRequest;
import com.bisttrading.broker.algolab.model.OrderResponse;
import com.bisttrading.broker.algolab.model.OrderStatus;
import com.bisttrading.broker.service.event.OrderEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTrackingService {

    private final OrderEventPublisher eventPublisher;
    private final ConcurrentMap<String, TrackedOrder> trackedOrders = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> clientOrderIdToOrderId = new ConcurrentHashMap<>();

    @Async
    public void startTracking(OrderRequest orderRequest) {
        String clientOrderId = orderRequest.getClientOrderId();

        TrackedOrder tracked = TrackedOrder.builder()
            .clientOrderId(clientOrderId)
            .symbol(orderRequest.getSymbol())
            .side(orderRequest.getSide())
            .type(orderRequest.getType())
            .quantity(orderRequest.getQuantity())
            .price(orderRequest.getPrice())
            .status(OrderStatus.NEW)
            .submittedAt(Instant.now())
            .lastUpdated(Instant.now())
            .build();

        trackedOrders.put(clientOrderId, tracked);
        log.debug("Started tracking order: {}", clientOrderId);
    }

    @Async
    public void updateOrderResponse(OrderResponse response) {
        String clientOrderId = response.getClientOrderId();
        String orderId = response.getOrderId();

        if (clientOrderId != null && orderId != null) {
            clientOrderIdToOrderId.put(clientOrderId, orderId);
        }

        TrackedOrder tracked = trackedOrders.get(clientOrderId);
        if (tracked != null) {
            // Update tracked order with response
            tracked.setOrderId(orderId);
            tracked.setStatus(response.getStatus());
            tracked.setFilledQuantity(response.getFilledQuantity());
            tracked.setRemainingQuantity(response.getRemainingQuantity());
            tracked.setAveragePrice(response.getAveragePrice());
            tracked.setCommission(response.getCommission());
            tracked.setLastUpdated(Instant.now());

            // Check for status changes
            OrderStatus previousStatus = tracked.getPreviousStatus();
            if (previousStatus != response.getStatus()) {
                tracked.setPreviousStatus(previousStatus);
                handleStatusChange(tracked, previousStatus, response.getStatus());
            }

            // Remove from tracking if order is final
            if (response.getStatus().isFinal()) {
                trackedOrders.remove(clientOrderId);
                if (orderId != null) {
                    clientOrderIdToOrderId.remove(clientOrderId);
                }
                log.debug("Stopped tracking completed order: {}", clientOrderId);
            }

            log.debug("Updated tracking for order: {} status: {}", clientOrderId, response.getStatus());
        }
    }

    @Async
    public void markOrderFailed(String clientOrderId, String reason) {
        TrackedOrder tracked = trackedOrders.get(clientOrderId);
        if (tracked != null) {
            tracked.setStatus(OrderStatus.REJECTED);
            tracked.setRejectReason(reason);
            tracked.setLastUpdated(Instant.now());

            eventPublisher.publishOrderRejected(tracked, reason);

            // Remove from tracking
            trackedOrders.remove(clientOrderId);
            log.debug("Marked order as failed: {} reason: {}", clientOrderId, reason);
        }
    }

    private void handleStatusChange(TrackedOrder tracked, OrderStatus oldStatus, OrderStatus newStatus) {
        log.info("Order status changed: {} {} -> {}",
                tracked.getClientOrderId(), oldStatus, newStatus);

        // Publish appropriate events based on status change
        switch (newStatus) {
            case PARTIALLY_FILLED -> eventPublisher.publishOrderPartiallyFilled(tracked);
            case FILLED -> eventPublisher.publishOrderFilled(tracked);
            case CANCELLED -> eventPublisher.publishOrderCancelled(tracked);
            case REJECTED -> eventPublisher.publishOrderRejected(tracked, tracked.getRejectReason());
            case EXPIRED -> eventPublisher.publishOrderExpired(tracked);
        }
    }

    public TrackedOrder getTrackedOrder(String clientOrderId) {
        return trackedOrders.get(clientOrderId);
    }

    public TrackedOrder getTrackedOrderByOrderId(String orderId) {
        return trackedOrders.values().stream()
            .filter(order -> orderId.equals(order.getOrderId()))
            .findFirst()
            .orElse(null);
    }

    public int getActiveOrderCount() {
        return trackedOrders.size();
    }

    // Clean up stale orders periodically
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void cleanupStaleOrders() {
        Instant cutoff = Instant.now().minusSeconds(3600); // 1 hour ago

        trackedOrders.entrySet().removeIf(entry -> {
            TrackedOrder order = entry.getValue();
            if (order.getLastUpdated().isBefore(cutoff)) {
                log.warn("Removing stale tracked order: {} last updated: {}",
                        order.getClientOrderId(), order.getLastUpdated());
                return true;
            }
            return false;
        });
    }

    @lombok.Data
    @lombok.Builder
    public static class TrackedOrder {
        private String clientOrderId;
        private String orderId;
        private String symbol;
        private com.bisttrading.broker.algolab.model.OrderSide side;
        private com.bisttrading.broker.algolab.model.OrderType type;
        private Integer quantity;
        private java.math.BigDecimal price;
        private OrderStatus status;
        private OrderStatus previousStatus;
        private Integer filledQuantity;
        private Integer remainingQuantity;
        private java.math.BigDecimal averagePrice;
        private java.math.BigDecimal commission;
        private String rejectReason;
        private Instant submittedAt;
        private Instant lastUpdated;
    }
}