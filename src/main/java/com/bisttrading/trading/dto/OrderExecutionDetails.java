package com.bisttrading.trading.dto;

import com.bisttrading.entity.trading.enums.ExecutionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

/**
 * DTO for order execution details.
 * Used when recording executions for an order.
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Order execution details")
public class OrderExecutionDetails {

    /**
     * Execution ID from broker/exchange
     */
    @Schema(description = "Execution ID", example = "EXEC-123456", required = true)
    private String executionId;

    /**
     * Broker execution ID
     */
    @Schema(description = "Broker execution ID", example = "BRK-EXEC-789")
    private String brokerExecutionId;

    /**
     * Trade ID from exchange
     */
    @Schema(description = "Exchange trade ID", example = "TRD-456789")
    private String tradeId;

    /**
     * Execution type
     */
    @Schema(description = "Execution type", example = "FILL", required = true)
    private ExecutionType executionType;

    /**
     * Quantity executed
     */
    @Schema(description = "Execution quantity", example = "500", required = true)
    private Integer quantity;

    /**
     * Execution price
     */
    @Schema(description = "Execution price", example = "15.75", required = true)
    private BigDecimal price;

    /**
     * Gross amount (quantity * price)
     */
    @Schema(description = "Gross amount", example = "7875.00", required = true)
    private BigDecimal grossAmount;

    /**
     * Commission charged
     */
    @Schema(description = "Commission", example = "7.88")
    private BigDecimal commission;

    /**
     * Brokerage fee
     */
    @Schema(description = "Brokerage fee", example = "3.94")
    private BigDecimal brokerageFee;

    /**
     * Exchange fee
     */
    @Schema(description = "Exchange fee", example = "0.79")
    private BigDecimal exchangeFee;

    /**
     * Tax amount
     */
    @Schema(description = "Tax amount", example = "15.75")
    private BigDecimal tax;

    /**
     * Net amount (gross ± fees)
     */
    @Schema(description = "Net amount", example = "7847.64")
    private BigDecimal netAmount;

    /**
     * Best bid at execution time
     */
    @Schema(description = "Bid price at execution", example = "15.74")
    private BigDecimal bidPrice;

    /**
     * Best ask at execution time
     */
    @Schema(description = "Ask price at execution", example = "15.76")
    private BigDecimal askPrice;

    /**
     * Market price at execution time
     */
    @Schema(description = "Market price at execution", example = "15.75")
    private BigDecimal marketPrice;

    /**
     * Counterparty broker
     */
    @Schema(description = "Counterparty broker", example = "BROKER-XYZ")
    private String contraBroker;

    /**
     * Execution timestamp
     */
    @Schema(description = "Execution timestamp", example = "2024-09-24T14:30:15Z", required = true)
    private ZonedDateTime executedAt;

    /**
     * Calculate total fees
     */
    public BigDecimal getTotalFees() {
        BigDecimal total = BigDecimal.ZERO;
        if (commission != null) total = total.add(commission);
        if (brokerageFee != null) total = total.add(brokerageFee);
        if (exchangeFee != null) total = total.add(exchangeFee);
        if (tax != null) total = total.add(tax);
        return total;
    }

    /**
     * Validate execution details
     */
    public void validate() {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Execution quantity must be positive");
        }

        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Execution price must be positive");
        }

        if (grossAmount == null || grossAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Gross amount must be positive");
        }

        if (executedAt == null) {
            throw new IllegalArgumentException("Execution timestamp is required");
        }

        // Validate gross amount calculation
        BigDecimal calculatedGross = price.multiply(BigDecimal.valueOf(quantity));
        if (grossAmount.compareTo(calculatedGross) != 0) {
            throw new IllegalArgumentException(
                String.format("Gross amount mismatch: expected %s, got %s", calculatedGross, grossAmount)
            );
        }
    }
}
