package com.bisttrading.oms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Batch Order Response DTO containing results of batch operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Batch order response with processing results")
public class BatchOrderResponse {

    @Schema(description = "Batch request identifier", example = "BATCH-20231201-001")
    private String batchId;

    @Schema(description = "Number of orders processed successfully")
    private int successCount;

    @Schema(description = "Number of orders that failed")
    private int errorCount;

    @Schema(description = "Total number of orders in batch")
    private int totalCount;

    @Schema(description = "Batch processing status")
    private BatchStatus status;

    @Schema(description = "Batch processing start time")
    private LocalDateTime startedAt;

    @Schema(description = "Batch processing completion time")
    private LocalDateTime completedAt;

    @Schema(description = "Total processing time in milliseconds")
    private Long processingTimeMs;

    @Schema(description = "Successfully processed orders")
    private List<OrderResult> successfulOrders;

    @Schema(description = "Failed orders with error details")
    private List<OrderError> failedOrders;

    @Schema(description = "Overall batch error message if applicable")
    private String errorMessage;

    public enum BatchStatus {
        PROCESSING,
        COMPLETED,
        PARTIAL_SUCCESS,
        FAILED,
        CANCELLED
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OrderResult {
        @Schema(description = "Index of order in original batch")
        private int orderIndex;

        @Schema(description = "Client order ID if provided")
        private String clientOrderId;

        @Schema(description = "Generated order response")
        private OrderResponse order;

        @Schema(description = "Processing time for this order in milliseconds")
        private Long processingTimeMs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OrderError {
        @Schema(description = "Index of order in original batch")
        private int orderIndex;

        @Schema(description = "Client order ID if provided")
        private String clientOrderId;

        @Schema(description = "Error code")
        private String errorCode;

        @Schema(description = "Error message")
        private String errorMessage;

        @Schema(description = "Field validation errors")
        private List<FieldError> fieldErrors;

        @Schema(description = "Original order request that failed")
        private OrderRequest originalRequest;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldError {
        @Schema(description = "Field name that failed validation")
        private String field;

        @Schema(description = "Validation error message")
        private String message;

        @Schema(description = "Rejected value")
        private Object rejectedValue;
    }

    /**
     * Calculate success rate percentage.
     */
    public double getSuccessRate() {
        if (totalCount == 0) return 0.0;
        return (double) successCount / totalCount * 100.0;
    }

    /**
     * Check if batch completed successfully.
     */
    public boolean isSuccess() {
        return status == BatchStatus.COMPLETED && errorCount == 0;
    }

    /**
     * Check if batch had partial success.
     */
    public boolean isPartialSuccess() {
        return status == BatchStatus.PARTIAL_SUCCESS ||
               (successCount > 0 && errorCount > 0);
    }
}