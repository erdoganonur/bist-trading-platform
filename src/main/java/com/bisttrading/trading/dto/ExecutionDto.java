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
 * DTO for order execution information.
 * Used in order history to show detailed execution breakdown.
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
public class ExecutionDto {

    /**
     * Execution ID
     */
    @Schema(description = "Execution ID", example = "exec-123456")
    private String executionId;

    /**
     * Trade ID from exchange
     */
    @Schema(description = "Exchange trade ID", example = "TRD-789456")
    private String tradeId;

    /**
     * Broker execution ID
     */
    @Schema(description = "Broker execution ID", example = "BRK-EXEC-456")
    private String brokerExecutionId;

    /**
     * Execution type
     */
    @Schema(description = "Execution type", example = "FILL")
    private ExecutionType executionType;

    /**
     * Quantity executed
     */
    @Schema(description = "Executed quantity", example = "500")
    private Integer executionQuantity;

    /**
     * Execution price
     */
    @Schema(description = "Execution price", example = "15.75")
    private BigDecimal executionPrice;

    /**
     * Gross amount (quantity * price)
     */
    @Schema(description = "Gross amount", example = "7875.00")
    private BigDecimal grossAmount;

    /**
     * Commission for this execution
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
    private BigDecimal taxAmount;

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
    @Schema(description = "Execution timestamp", example = "2024-09-24T14:30:15Z")
    private ZonedDateTime executionTime;

    /**
     * Calculate total fees for this execution
     */
    public BigDecimal getTotalFees() {
        BigDecimal total = BigDecimal.ZERO;
        if (commission != null) total = total.add(commission);
        if (brokerageFee != null) total = total.add(brokerageFee);
        if (exchangeFee != null) total = total.add(exchangeFee);
        if (taxAmount != null) total = total.add(taxAmount);
        return total;
    }
}
