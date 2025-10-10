package com.bisttrading.trading.dto;

import com.bisttrading.entity.trading.enums.OrderSide;
import com.bisttrading.entity.trading.enums.OrderStatus;
import com.bisttrading.entity.trading.enums.OrderType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Search criteria for filtering orders.
 * Used for advanced order queries with multiple filters.
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Order search and filter criteria")
public class OrderSearchCriteria {

    /**
     * Filter by symbol code
     */
    @Schema(description = "Symbol code filter", example = "AKBNK")
    private String symbol;

    /**
     * Filter by order side
     */
    @Schema(description = "Order side filter (BUY/SELL)", example = "BUY")
    private OrderSide orderSide;

    /**
     * Filter by order type
     */
    @Schema(description = "Order type filter", example = "LIMIT")
    private OrderType orderType;

    /**
     * Filter by order status
     */
    @Schema(description = "Order status filter", example = "FILLED")
    private OrderStatus orderStatus;

    /**
     * Filter by multiple statuses
     */
    @Schema(description = "Multiple order statuses filter", example = "[\"SUBMITTED\", \"ACCEPTED\"]")
    private List<OrderStatus> orderStatuses;

    /**
     * Filter by client order ID
     */
    @Schema(description = "Client order ID filter", example = "ORD-12345")
    private String clientOrderId;

    /**
     * Filter by broker order ID
     */
    @Schema(description = "Broker order ID filter", example = "BRK-98765")
    private String brokerOrderId;

    /**
     * Filter by account ID
     */
    @Schema(description = "Account ID filter", example = "ACC-1001")
    private String accountId;

    /**
     * Filter orders created after this date
     */
    @Schema(description = "Created after date filter", example = "2024-01-01T00:00:00Z")
    private ZonedDateTime createdAfter;

    /**
     * Filter orders created before this date
     */
    @Schema(description = "Created before date filter", example = "2024-12-31T23:59:59Z")
    private ZonedDateTime createdBefore;

    /**
     * Filter by minimum order quantity
     */
    @Schema(description = "Minimum quantity filter", example = "100")
    private Integer minQuantity;

    /**
     * Filter by maximum order quantity
     */
    @Schema(description = "Maximum quantity filter", example = "10000")
    private Integer maxQuantity;

    /**
     * Filter by minimum price
     */
    @Schema(description = "Minimum price filter", example = "10.00")
    private BigDecimal minPrice;

    /**
     * Filter by maximum price
     */
    @Schema(description = "Maximum price filter", example = "100.00")
    private BigDecimal maxPrice;

    /**
     * Only return active orders
     */
    @Schema(description = "Return only active orders", example = "true", defaultValue = "false")
    @Builder.Default
    private Boolean activeOnly = false;

    /**
     * Only return filled orders
     */
    @Schema(description = "Return only filled orders", example = "false", defaultValue = "false")
    @Builder.Default
    private Boolean filledOnly = false;

    /**
     * Filter by strategy ID
     */
    @Schema(description = "Strategy ID filter", example = "STRAT-001")
    private String strategyId;

    /**
     * Filter by algorithm ID
     */
    @Schema(description = "Algorithm ID filter", example = "ALGO-001")
    private String algoId;

    /**
     * Check if any filters are applied
     */
    public boolean hasFilters() {
        return symbol != null
            || orderSide != null
            || orderType != null
            || orderStatus != null
            || (orderStatuses != null && !orderStatuses.isEmpty())
            || clientOrderId != null
            || brokerOrderId != null
            || accountId != null
            || createdAfter != null
            || createdBefore != null
            || minQuantity != null
            || maxQuantity != null
            || minPrice != null
            || maxPrice != null
            || strategyId != null
            || algoId != null;
    }

    /**
     * Check if date range filter is applied
     */
    public boolean hasDateRange() {
        return createdAfter != null || createdBefore != null;
    }

    /**
     * Check if quantity range filter is applied
     */
    public boolean hasQuantityRange() {
        return minQuantity != null || maxQuantity != null;
    }

    /**
     * Check if price range filter is applied
     */
    public boolean hasPriceRange() {
        return minPrice != null || maxPrice != null;
    }
}
