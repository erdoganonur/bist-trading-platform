package com.bisttrading.oms.service;

import com.bisttrading.oms.model.CreateOrderRequest;
import com.bisttrading.oms.model.ModifyOrderRequest;
import com.bisttrading.oms.model.OMSOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service for validating order operations
 */
@Slf4j
@Service
public class OrderValidationService {

    public void validateCreateOrder(CreateOrderRequest request) {
        validateBasicOrderInfo(request.getSymbol(), request.getSide(), request.getType(),
                              request.getQuantity(), request.getTimeInForce());

        if (request.getType() == OMSOrder.OrderType.LIMIT || request.getType() == OMSOrder.OrderType.STOP_LIMIT) {
            if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Price is required for limit orders");
            }
        }

        if (request.getType() == OMSOrder.OrderType.STOP || request.getType() == OMSOrder.OrderType.STOP_LIMIT) {
            if (request.getStopPrice() == null || request.getStopPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Stop price is required for stop orders");
            }
        }

        log.debug("Order creation validation passed for symbol: {}", request.getSymbol());
    }

    public void validateModifyOrder(OMSOrder existingOrder, ModifyOrderRequest request) {
        if (!existingOrder.isActive()) {
            throw new IllegalArgumentException("Cannot modify inactive order: " + existingOrder.getOrderId());
        }

        if (request.getQuantity() != null) {
            if (request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than 0");
            }
            if (request.getQuantity().compareTo(existingOrder.getFilledQuantity()) < 0) {
                throw new IllegalArgumentException("New quantity cannot be less than filled quantity");
            }
        }

        if (request.getPrice() != null && request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be greater than 0");
        }

        log.debug("Order modification validation passed for order: {}", existingOrder.getOrderId());
    }

    public void validateCancelOrder(OMSOrder order) {
        if (!order.isActive()) {
            throw new IllegalArgumentException("Cannot cancel inactive order: " + order.getOrderId());
        }

        log.debug("Order cancellation validation passed for order: {}", order.getOrderId());
    }

    private void validateBasicOrderInfo(String symbol, OMSOrder.OrderSide side, OMSOrder.OrderType type,
                                       BigDecimal quantity, OMSOrder.TimeInForce timeInForce) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be empty");
        }

        if (side == null) {
            throw new IllegalArgumentException("Order side is required");
        }

        if (type == null) {
            throw new IllegalArgumentException("Order type is required");
        }

        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        if (timeInForce == null) {
            throw new IllegalArgumentException("Time in force is required");
        }
    }

    public boolean isValidSymbol(String symbol) {
        return symbol != null && symbol.trim().length() >= 3 && symbol.trim().length() <= 20;
    }

    public boolean isValidQuantity(BigDecimal quantity) {
        return quantity != null && quantity.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isValidPrice(BigDecimal price) {
        return price != null && price.compareTo(BigDecimal.ZERO) > 0;
    }
}