package com.bisttrading.oms.dto;

import com.bisttrading.oms.model.OMSOrder;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.hateoas.RepresentationModel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Order Response DTO with HATEOAS support for BIST Trading Platform.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Order response with complete order information")
@EqualsAndHashCode(callSuper = false)
public class OrderResponse extends RepresentationModel<OrderResponse> {

    @Schema(description = "Unique order identifier", example = "OMS-1695123456-abc12345")
    private String orderId;

    @Schema(description = "Client provided order identifier", example = "USER123-1695123456")
    private String clientOrderId;

    @Schema(description = "External broker order identifier", example = "BROKER-456789")
    private String externalOrderId;

    @Schema(description = "User identifier who owns this order", example = "user123")
    private String userId;

    @Schema(description = "Trading symbol", example = "THYAO")
    private String symbol;

    @Schema(description = "Order side", example = "BUY")
    private OMSOrder.OrderSide side;

    @Schema(description = "Order type", example = "LIMIT")
    private OMSOrder.OrderType type;

    @Schema(description = "Order quantity", example = "1000.00")
    private BigDecimal quantity;

    @Schema(description = "Order price (for limit orders)", example = "45.75")
    private BigDecimal price;

    @Schema(description = "Stop price (for stop orders)", example = "44.00")
    private BigDecimal stopPrice;

    @Schema(description = "Current order status", example = "NEW")
    private OMSOrder.OrderStatus status;

    @Schema(description = "Time in force", example = "GTC")
    private OMSOrder.TimeInForce timeInForce;

    @Schema(description = "Filled quantity", example = "500.00")
    private BigDecimal filledQuantity;

    @Schema(description = "Remaining quantity", example = "500.00")
    private BigDecimal remainingQuantity;

    @Schema(description = "Average fill price", example = "45.80")
    private BigDecimal averagePrice;

    @Schema(description = "Total commission paid", example = "2.29")
    private BigDecimal commission;

    @Schema(description = "Rejection reason if order was rejected")
    private String rejectReason;

    @Schema(description = "Cancellation reason if order was cancelled")
    private String cancelReason;

    @Schema(description = "Order creation timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Order last update timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    @Schema(description = "Order fill timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime filledAt;

    @Schema(description = "Order cancellation timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime cancelledAt;

    @Schema(description = "Order expiry time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expireTime;

    @Schema(description = "Account identifier")
    private String accountId;

    @Schema(description = "Portfolio identifier")
    private String portfolioId;

    @Schema(description = "Strategy identifier")
    private String strategyId;

    @Schema(description = "Order notes")
    private String notes;

    @Schema(description = "Whether order is active")
    private boolean active;

    // Calculated fields
    @Schema(description = "Total order value (filledQuantity * averagePrice)", example = "22900.00")
    private BigDecimal totalValue;

    @Schema(description = "Fill percentage", example = "50.00")
    private BigDecimal fillPercentage;

    @Schema(description = "Order executions if available")
    private List<OrderExecutionDto> executions;

    // Metadata
    @Schema(description = "Order metadata for additional information")
    private OrderMetadata metadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OrderExecutionDto {
        @Schema(description = "Execution identifier")
        private String executionId;

        @Schema(description = "Executed quantity")
        private BigDecimal quantity;

        @Schema(description = "Execution price")
        private BigDecimal price;

        @Schema(description = "Execution timestamp")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime executedAt;

        @Schema(description = "Execution commission")
        private BigDecimal commission;

        @Schema(description = "Counterparty identifier")
        private String counterparty;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OrderMetadata {
        @Schema(description = "Source of the order")
        private String source;

        @Schema(description = "Order priority")
        private Integer priority;

        @Schema(description = "Risk score at time of placement")
        private BigDecimal riskScore;

        @Schema(description = "Whether order was placed during market hours")
        private Boolean duringMarketHours;

        @Schema(description = "Estimated settlement date")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDateTime estimatedSettlement;
    }

    /**
     * Factory method to create OrderResponse from OMSOrder entity.
     */
    public static OrderResponse fromEntity(OMSOrder order) {
        return OrderResponse.builder()
            .orderId(order.getOrderId())
            .clientOrderId(order.getClientOrderId())
            .externalOrderId(order.getExternalOrderId())
            .userId(order.getUserId())
            .symbol(order.getSymbol())
            .side(order.getSide())
            .type(order.getType())
            .quantity(order.getQuantity())
            .price(order.getPrice())
            .stopPrice(order.getStopPrice())
            .status(order.getStatus())
            .timeInForce(order.getTimeInForce())
            .filledQuantity(order.getFilledQuantity())
            .remainingQuantity(order.getRemainingQuantity())
            .averagePrice(order.getAveragePrice())
            .commission(order.getCommission())
            .rejectReason(order.getRejectReason())
            .cancelReason(order.getCancelReason())
            .createdAt(order.getCreatedAt())
            .updatedAt(order.getUpdatedAt())
            .filledAt(order.getFilledAt())
            .cancelledAt(order.getCancelledAt())
            .expireTime(order.getExpireTime())
            .accountId(order.getAccountId())
            .portfolioId(order.getPortfolioId())
            .strategyId(order.getStrategyId())
            .notes(order.getNotes())
            .active(order.isActive())
            .totalValue(order.getTotalValue())
            .fillPercentage(order.getFillPercentage())
            .build();
    }
}