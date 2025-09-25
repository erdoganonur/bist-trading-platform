package com.bisttrading.oms.event;

import com.bisttrading.oms.model.OMSOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Event publisher for OMS events
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OMSEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publishOrderCreated(OMSOrder order) {
        OrderCreatedEvent event = OrderCreatedEvent.builder()
            .orderId(order.getOrderId())
            .userId(order.getUserId())
            .symbol(order.getSymbol())
            .side(order.getSide())
            .type(order.getType())
            .quantity(order.getQuantity())
            .price(order.getPrice())
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published OrderCreated event for order: {}", order.getOrderId());
    }

    public void publishOrderCancelled(OMSOrder order, String reason) {
        OrderCancelledEvent event = OrderCancelledEvent.builder()
            .orderId(order.getOrderId())
            .userId(order.getUserId())
            .symbol(order.getSymbol())
            .reason(reason)
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published OrderCancelled event for order: {}", order.getOrderId());
    }

    public void publishOrderModified(OMSOrder order, String reason) {
        OrderModifiedEvent event = OrderModifiedEvent.builder()
            .orderId(order.getOrderId())
            .userId(order.getUserId())
            .symbol(order.getSymbol())
            .reason(reason)
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published OrderModified event for order: {}", order.getOrderId());
    }

    public void publishOrderFilled(OMSOrder order) {
        OrderFilledEvent event = OrderFilledEvent.builder()
            .orderId(order.getOrderId())
            .userId(order.getUserId())
            .symbol(order.getSymbol())
            .filledQuantity(order.getFilledQuantity())
            .averagePrice(order.getAveragePrice())
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published OrderFilled event for order: {}", order.getOrderId());
    }

    public void publishOrderRejected(OMSOrder order, String reason) {
        OrderRejectedEvent event = OrderRejectedEvent.builder()
            .orderId(order.getOrderId())
            .userId(order.getUserId())
            .symbol(order.getSymbol())
            .reason(reason)
            .build();

        applicationEventPublisher.publishEvent(event);
        log.debug("Published OrderRejected event for order: {}", order.getOrderId());
    }
}