package com.bisttrading.broker.service.event;

import com.bisttrading.broker.algolab.model.OrderResponse;
import com.bisttrading.broker.algolab.model.OrderSubmissionRequest;
import com.bisttrading.broker.service.OrderTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Async
    public void publishOrderSubmitted(OrderResponse orderResponse) {
        OrderSubmittedEvent event = OrderSubmittedEvent.builder()
            .orderId(orderResponse.getOrderId())
            .clientOrderId(orderResponse.getClientOrderId())
            .symbol(orderResponse.getSymbol())
            .side(orderResponse.getSide())
            .type(orderResponse.getType())
            .quantity(orderResponse.getQuantity())
            .price(orderResponse.getPrice())
            .status(orderResponse.getStatus())
            .timestamp(orderResponse.getCreatedAt())
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published OrderSubmittedEvent for order: {}", orderResponse.getOrderId());
    }

    @Async
    public void publishOrderRejected(OrderSubmissionRequest request, String reason) {
        OrderRejectedEvent event = OrderRejectedEvent.builder()
            .userId(request.getUserId())
            .symbol(request.getSymbol())
            .side(request.getSide())
            .type(request.getOrderType())
            .quantity(request.getQuantity())
            .price(request.getPrice())
            .reason(reason)
            .timestamp(java.time.Instant.now())
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published OrderRejectedEvent for user: {} symbol: {}", request.getUserId(), request.getSymbol());
    }

    @Async
    public void publishOrderRejected(OrderTrackingService.TrackedOrder trackedOrder, String reason) {
        OrderRejectedEvent event = OrderRejectedEvent.builder()
            .clientOrderId(trackedOrder.getClientOrderId())
            .orderId(trackedOrder.getOrderId())
            .symbol(trackedOrder.getSymbol())
            .side(trackedOrder.getSide())
            .type(trackedOrder.getType())
            .quantity(trackedOrder.getQuantity())
            .price(trackedOrder.getPrice())
            .reason(reason)
            .timestamp(java.time.Instant.now())
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published OrderRejectedEvent for tracked order: {}", trackedOrder.getClientOrderId());
    }

    @Async
    public void publishOrderPartiallyFilled(OrderTrackingService.TrackedOrder trackedOrder) {
        OrderPartiallyFilledEvent event = OrderPartiallyFilledEvent.builder()
            .orderId(trackedOrder.getOrderId())
            .clientOrderId(trackedOrder.getClientOrderId())
            .symbol(trackedOrder.getSymbol())
            .side(trackedOrder.getSide())
            .originalQuantity(trackedOrder.getQuantity())
            .filledQuantity(trackedOrder.getFilledQuantity())
            .remainingQuantity(trackedOrder.getRemainingQuantity())
            .averagePrice(trackedOrder.getAveragePrice())
            .commission(trackedOrder.getCommission())
            .timestamp(java.time.Instant.now())
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published OrderPartiallyFilledEvent for order: {}", trackedOrder.getOrderId());
    }

    @Async
    public void publishOrderFilled(OrderTrackingService.TrackedOrder trackedOrder) {
        OrderFilledEvent event = OrderFilledEvent.builder()
            .orderId(trackedOrder.getOrderId())
            .clientOrderId(trackedOrder.getClientOrderId())
            .symbol(trackedOrder.getSymbol())
            .side(trackedOrder.getSide())
            .quantity(trackedOrder.getQuantity())
            .averagePrice(trackedOrder.getAveragePrice())
            .totalValue(trackedOrder.getAveragePrice() != null && trackedOrder.getQuantity() != null ?
                       trackedOrder.getAveragePrice().multiply(java.math.BigDecimal.valueOf(trackedOrder.getQuantity())) :
                       null)
            .commission(trackedOrder.getCommission())
            .timestamp(java.time.Instant.now())
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published OrderFilledEvent for order: {}", trackedOrder.getOrderId());
    }

    @Async
    public void publishOrderCancelled(OrderResponse orderResponse) {
        OrderCancelledEvent event = OrderCancelledEvent.builder()
            .orderId(orderResponse.getOrderId())
            .clientOrderId(orderResponse.getClientOrderId())
            .symbol(orderResponse.getSymbol())
            .side(orderResponse.getSide())
            .originalQuantity(orderResponse.getQuantity())
            .filledQuantity(orderResponse.getFilledQuantity())
            .remainingQuantity(orderResponse.getRemainingQuantity())
            .timestamp(java.time.Instant.now())
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published OrderCancelledEvent for order: {}", orderResponse.getOrderId());
    }

    @Async
    public void publishOrderCancelled(OrderTrackingService.TrackedOrder trackedOrder) {
        OrderCancelledEvent event = OrderCancelledEvent.builder()
            .orderId(trackedOrder.getOrderId())
            .clientOrderId(trackedOrder.getClientOrderId())
            .symbol(trackedOrder.getSymbol())
            .side(trackedOrder.getSide())
            .originalQuantity(trackedOrder.getQuantity())
            .filledQuantity(trackedOrder.getFilledQuantity())
            .remainingQuantity(trackedOrder.getRemainingQuantity())
            .timestamp(java.time.Instant.now())
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published OrderCancelledEvent for tracked order: {}", trackedOrder.getClientOrderId());
    }

    @Async
    public void publishOrderModified(OrderResponse orderResponse) {
        OrderModifiedEvent event = OrderModifiedEvent.builder()
            .orderId(orderResponse.getOrderId())
            .clientOrderId(orderResponse.getClientOrderId())
            .symbol(orderResponse.getSymbol())
            .side(orderResponse.getSide())
            .type(orderResponse.getType())
            .quantity(orderResponse.getQuantity())
            .price(orderResponse.getPrice())
            .status(orderResponse.getStatus())
            .timestamp(orderResponse.getUpdatedAt())
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published OrderModifiedEvent for order: {}", orderResponse.getOrderId());
    }

    @Async
    public void publishOrderExpired(OrderTrackingService.TrackedOrder trackedOrder) {
        OrderExpiredEvent event = OrderExpiredEvent.builder()
            .orderId(trackedOrder.getOrderId())
            .clientOrderId(trackedOrder.getClientOrderId())
            .symbol(trackedOrder.getSymbol())
            .side(trackedOrder.getSide())
            .originalQuantity(trackedOrder.getQuantity())
            .filledQuantity(trackedOrder.getFilledQuantity())
            .remainingQuantity(trackedOrder.getRemainingQuantity())
            .timestamp(java.time.Instant.now())
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published OrderExpiredEvent for order: {}", trackedOrder.getOrderId());
    }
}