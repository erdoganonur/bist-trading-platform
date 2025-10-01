package com.bisttrading.oms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Batch Order Request DTO for submitting multiple orders at once.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Batch order request for multiple orders")
public class BatchOrderRequest {

    @NotEmpty(message = "Orders list cannot be empty")
    @Size(max = 100, message = "Cannot submit more than 100 orders in a batch")
    @Valid
    @Schema(description = "List of orders to be processed", required = true)
    private List<OrderRequest> orders;

    @Schema(description = "Whether to stop processing on first error", example = "false")
    @Builder.Default
    private Boolean stopOnError = false;

    @Schema(description = "Whether all orders should be validated before processing", example = "true")
    @Builder.Default
    private Boolean validateAll = true;

    @Schema(description = "Request identifier for tracking", example = "BATCH-20231201-001")
    private String batchId;

    @Schema(description = "Priority for the entire batch")
    private Integer batchPriority;

    @Schema(description = "Notes for the batch operation")
    @Size(max = 500, message = "Batch notes cannot exceed 500 characters")
    private String notes;
}