package com.bisttrading.broker.service;

import com.bisttrading.broker.algolab.AlgoLabApiClient;
import com.bisttrading.broker.algolab.model.*;
import com.bisttrading.broker.algolab.model.response.CancelOrderResponse;
import com.bisttrading.broker.algolab.exception.*;
import com.bisttrading.broker.service.event.OrderEventPublisher;
import com.bisttrading.broker.service.validation.OrderValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProcessingService {

    private final AlgoLabApiClient algoLabClient;
    private final OrderValidator orderValidator;
    private final OrderEventPublisher eventPublisher;
    private final OrderTrackingService orderTrackingService;
    private final RiskManagementService riskManagementService;

    @Transactional
    public CompletableFuture<OrderResponse> submitOrder(OrderSubmissionRequest request) {
        log.info("Processing order submission for user: {} symbol: {} side: {} quantity: {}",
                request.getUserId(), request.getSymbol(), request.getSide(), request.getQuantity());

        return CompletableFuture
            .supplyAsync(() -> validateAndPrepareOrder(request))
            .thenCompose(this::checkRiskLimits)
            .thenCompose(this::executeOrder)
            .thenApply(this::processOrderResponse)
            .whenComplete((response, throwable) -> {
                if (throwable != null) {
                    log.error("Order submission failed for user: {}", request.getUserId(), throwable);
                    eventPublisher.publishOrderRejected(request, throwable.getMessage());
                }
            });
    }

    private OrderRequest validateAndPrepareOrder(OrderSubmissionRequest request) {
        try {
            // Validate basic order parameters
            orderValidator.validateOrderSubmission(request);

            // Generate client order ID
            String clientOrderId = generateClientOrderId(request.getUserId());

            // Build AlgoLab order request
            OrderRequest orderRequest = OrderRequest.builder()
                .symbol(request.getSymbol())
                .side(request.getSide())
                .type(request.getOrderType())
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .stopPrice(request.getStopPrice())
                .timeInForce(request.getTimeInForce())
                .clientOrderId(clientOrderId)
                .accountId(request.getAccountId())
                .goodTillDate(request.getGoodTillDate())
                .icebergQuantity(request.getIcebergQuantity())
                .instruction(request.getInstruction())
                .marginBuy(request.getMarginBuy())
                .shortSale(request.getShortSale())
                .build();

            // Validate the built order
            orderRequest.validate();

            log.debug("Order prepared with client order ID: {}", clientOrderId);
            return orderRequest;

        } catch (Exception e) {
            log.error("Order validation failed", e);
            throw new AlgoLabValidationException("Order validation failed: " + e.getMessage(), e);
        }
    }

    private CompletableFuture<OrderRequest> checkRiskLimits(OrderRequest orderRequest) {
        return riskManagementService.validateOrderRisk(orderRequest)
            .thenApply(riskResult -> {
                if (!riskResult.isApproved()) {
                    throw new AlgoLabValidationException(
                        "Order rejected by risk management: " + riskResult.getReason()
                    );
                }
                return orderRequest;
            });
    }

    private CompletableFuture<OrderResponse> executeOrder(OrderRequest orderRequest) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Executing order: {} {} {} at {}",
                        orderRequest.getSide(),
                        orderRequest.getQuantity(),
                        orderRequest.getSymbol(),
                        orderRequest.getPrice());

                // Start tracking the order
                orderTrackingService.startTracking(orderRequest);

                // Submit to AlgoLab
                OrderResponse response = algoLabClient.placeOrder(orderRequest);

                log.info("Order submitted successfully. Order ID: {} Client Order ID: {}",
                        response.getOrderId(), response.getClientOrderId());

                return response;

            } catch (Exception e) {
                log.error("Order execution failed for client order: {}", orderRequest.getClientOrderId(), e);
                orderTrackingService.markOrderFailed(orderRequest.getClientOrderId(), e.getMessage());
                throw AlgoLabErrorHandler.handleException(e);
            }
        });
    }

    private OrderResponse processOrderResponse(OrderResponse response) {
        try {
            // Update tracking with order response
            orderTrackingService.updateOrderResponse(response);

            // Publish order submitted event
            eventPublisher.publishOrderSubmitted(response);

            log.info("Order processing completed. Order ID: {} Status: {}",
                    response.getOrderId(), response.getStatus());

            return response;

        } catch (Exception e) {
            log.error("Error processing order response", e);
            throw new AlgoLabException("Failed to process order response: " + e.getMessage(), e);
        }
    }

    public CompletableFuture<OrderResponse> cancelOrder(String orderId, String userId) {
        log.info("Processing order cancellation for order ID: {} user: {}", orderId, userId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate cancellation request
                orderValidator.validateOrderCancellation(orderId, userId);

                // Cancel order via AlgoLab
                CancelOrderResponse cancelResponse = algoLabClient.cancelOrder(orderId);

                // Update tracking - create an OrderResponse from CancelOrderResponse
                OrderResponse response = OrderResponse.builder()
                    .orderId(cancelResponse.getOrderId())
                    .status(OrderStatus.fromValue(cancelResponse.getStatus()))
                    .success(cancelResponse.isSuccess())
                    .build();

                orderTrackingService.updateOrderResponse(response);

                // Publish cancellation event
                eventPublisher.publishOrderCancelled(response);

                log.info("Order cancellation completed. Order ID: {} Status: {}",
                        response.getOrderId(), response.getStatus());

                return response;

            } catch (Exception e) {
                log.error("Order cancellation failed for order ID: {}", orderId, e);
                throw AlgoLabErrorHandler.handleException(e);
            }
        });
    }

    public CompletableFuture<OrderResponse> modifyOrder(String orderId, OrderModificationRequest request) {
        log.info("Processing order modification for order ID: {}", orderId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate modification request
                orderValidator.validateOrderModification(orderId, request);

                // Modify order via AlgoLab
                OrderResponse response = algoLabClient.modifyOrder(orderId, request);

                // Update tracking
                orderTrackingService.updateOrderResponse(response);

                // Publish modification event
                eventPublisher.publishOrderModified(response);

                log.info("Order modification completed. Order ID: {} Status: {}",
                        response.getOrderId(), response.getStatus());

                return response;

            } catch (Exception e) {
                log.error("Order modification failed for order ID: {}", orderId, e);
                throw AlgoLabErrorHandler.handleException(e);
            }
        });
    }

    public CompletableFuture<List<OrderResponse>> getActiveOrders(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return algoLabClient.getOrders(userId, "ACTIVE");
            } catch (Exception e) {
                log.error("Failed to get active orders for user: {}", userId, e);
                throw AlgoLabErrorHandler.handleException(e);
            }
        });
    }

    public CompletableFuture<OrderStatus> getOrderStatus(String orderId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return algoLabClient.getOrderStatus(orderId);
            } catch (Exception e) {
                log.error("Failed to get order status for order ID: {}", orderId, e);
                throw AlgoLabErrorHandler.handleException(e);
            }
        });
    }

    private String generateClientOrderId(String userId) {
        return String.format("%s-%d-%s",
                userId,
                Instant.now().toEpochMilli(),
                UUID.randomUUID().toString().substring(0, 8));
    }

    // Market order convenience methods
    public CompletableFuture<OrderResponse> submitMarketOrder(String userId, String accountId,
                                                              String symbol, OrderSide side, Integer quantity) {
        OrderSubmissionRequest request = OrderSubmissionRequest.builder()
            .userId(userId)
            .accountId(accountId)
            .symbol(symbol)
            .side(side)
            .orderType(OrderType.MARKET)
            .quantity(quantity)
            .timeInForce(TimeInForce.DAY)
            .build();

        return submitOrder(request);
    }

    // Limit order convenience methods
    public CompletableFuture<OrderResponse> submitLimitOrder(String userId, String accountId,
                                                             String symbol, OrderSide side,
                                                             Integer quantity, BigDecimal price) {
        OrderSubmissionRequest request = OrderSubmissionRequest.builder()
            .userId(userId)
            .accountId(accountId)
            .symbol(symbol)
            .side(side)
            .orderType(OrderType.LIMIT)
            .quantity(quantity)
            .price(price)
            .timeInForce(TimeInForce.DAY)
            .build();

        return submitOrder(request);
    }
}