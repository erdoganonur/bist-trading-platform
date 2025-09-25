package com.bisttrading.oms.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request for modifying an existing order
 */
@Data
@Builder
public class ModifyOrderRequest {

    @NotBlank(message = "User ID cannot be blank")
    private String userId;

    @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @DecimalMin(value = "0.01", message = "Stop price must be greater than 0")
    private BigDecimal stopPrice;

    private OMSOrder.TimeInForce timeInForce;

    private LocalDateTime expireTime;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;

    private String reason;

    // Helper methods
    public boolean hasQuantityChange() {
        return quantity != null;
    }

    public boolean hasPriceChange() {
        return price != null;
    }

    public boolean hasStopPriceChange() {
        return stopPrice != null;
    }

    public boolean hasTimeInForceChange() {
        return timeInForce != null;
    }

    public boolean hasExpireTimeChange() {
        return expireTime != null;
    }
}