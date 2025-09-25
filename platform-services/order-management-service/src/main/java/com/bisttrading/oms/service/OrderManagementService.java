package com.bisttrading.oms.service;

import com.bisttrading.oms.model.*;
import com.bisttrading.oms.repository.OrderRepository;
import com.bisttrading.oms.event.OMSEventPublisher;
import com.bisttrading.oms.exception.OrderNotFoundException;
import com.bisttrading.oms.exception.OrderManagementException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Order Management Service
 * Handles order lifecycle, validation, and coordination with broker services
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderManagementService {

    private final OrderRepository orderRepository;
    private final OMSEventPublisher eventPublisher;
    private final OrderValidationService orderValidationService;
    private final OrderAuditService orderAuditService;

    /**
     * Create a new order
     */
    @Transactional
    public CompletableFuture<OMSOrder> createOrder(CreateOrderRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate request
                orderValidationService.validateCreateOrder(request);

                // Create OMS order
                OMSOrder omsOrder = OMSOrder.builder()
                    .orderId(generateOrderId())
                    .clientOrderId(generateClientOrderId(request.getUserId()))
                    .userId(request.getUserId())
                    .symbol(request.getSymbol())
                    .side(request.getSide())
                    .type(request.getType())
                    .quantity(request.getQuantity())
                    .price(request.getPrice())
                    .stopPrice(request.getStopPrice())
                    .status(OMSOrder.OrderStatus.NEW)
                    .timeInForce(request.getTimeInForce())
                    .expireTime(request.getExpireTime())
                    .accountId(request.getAccountId())
                    .portfolioId(request.getPortfolioId())
                    .strategyId(request.getStrategyId())
                    .notes(request.getNotes())
                    .build();

                // Save order
                OMSOrder savedOrder = orderRepository.save(omsOrder);

                // Audit
                orderAuditService.auditOrderCreation(savedOrder);

                // Publish event
                eventPublisher.publishOrderCreated(savedOrder);

                log.info("Order created successfully: {}", savedOrder.getOrderId());
                return savedOrder;

            } catch (Exception e) {
                log.error("Failed to create order for user: {}", request.getUserId(), e);
                throw new OrderManagementException("Failed to create order: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Cancel an existing order
     */
    @Transactional
    public CompletableFuture<OMSOrder> cancelOrder(String orderId, String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                OMSOrder order = findOrderByIdAndUser(orderId, userId);
                orderValidationService.validateCancelOrder(order);

                order.setStatus(OMSOrder.OrderStatus.CANCELLED);
                order.setCancelledAt(LocalDateTime.now());

                OMSOrder savedOrder = orderRepository.save(order);

                orderAuditService.auditOrderCancellation(savedOrder, "User requested cancellation");
                eventPublisher.publishOrderCancelled(savedOrder, "User requested cancellation");

                log.info("Order cancelled successfully: {}", orderId);
                return savedOrder;

            } catch (Exception e) {
                log.error("Failed to cancel order: {}", orderId, e);
                throw new OrderManagementException("Failed to cancel order: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Modify an existing order
     */
    @Transactional
    public CompletableFuture<OMSOrder> modifyOrder(String orderId, ModifyOrderRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                OMSOrder order = findOrderByIdAndUser(orderId, request.getUserId());
                orderValidationService.validateModifyOrder(order, request);

                // Update fields
                if (request.hasQuantityChange()) {
                    order.setQuantity(request.getQuantity());
                }
                if (request.hasPriceChange()) {
                    order.setPrice(request.getPrice());
                }
                if (request.hasStopPriceChange()) {
                    order.setStopPrice(request.getStopPrice());
                }
                if (request.hasTimeInForceChange()) {
                    order.setTimeInForce(request.getTimeInForce());
                }
                if (request.hasExpireTimeChange()) {
                    order.setExpireTime(request.getExpireTime());
                }

                OMSOrder savedOrder = orderRepository.save(order);

                orderAuditService.auditOrderModification(order, savedOrder, request.getReason());
                eventPublisher.publishOrderModified(savedOrder, request.getReason());

                log.info("Order modified successfully: {}", orderId);
                return savedOrder;

            } catch (Exception e) {
                log.error("Failed to modify order: {}", orderId, e);
                throw new OrderManagementException("Failed to modify order: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Get orders for a user with pagination
     */
    public Page<OMSOrder> getOrdersForUser(String userId, OrderFilter filter, Pageable pageable) {
        try {
            // Simple implementation - extend as needed
            return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        } catch (Exception e) {
            log.error("Failed to get orders for user: {}", userId, e);
            throw new OrderManagementException("Failed to get orders: " + e.getMessage(), e);
        }
    }

    /**
     * Get order statistics for a user
     */
    public OrderStatistics getOrderStatistics(String userId, LocalDateTime fromDate, LocalDateTime toDate) {
        try {
            // Basic statistics implementation
            long totalOrders = orderRepository.count();
            long activeOrders = orderRepository.countActiveOrdersByUserId(userId);

            return OrderStatistics.builder()
                .userId(userId)
                .fromDate(fromDate)
                .toDate(toDate)
                .totalOrders(totalOrders)
                .activeOrders(activeOrders)
                .build();

        } catch (Exception e) {
            log.error("Failed to get order statistics for user: {}", userId, e);
            throw new OrderManagementException("Failed to get order statistics: " + e.getMessage(), e);
        }
    }

    private OMSOrder findOrderByIdAndUser(String orderId, String userId) {
        Optional<OMSOrder> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty() || !orderOpt.get().getUserId().equals(userId)) {
            throw new OrderNotFoundException("Order not found: " + orderId);
        }
        return orderOpt.get();
    }

    private String generateOrderId() {
        return "OMS-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String generateClientOrderId(String userId) {
        return userId + "-" + System.currentTimeMillis();
    }
}