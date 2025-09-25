package com.bisttrading.broker.algolab.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class OrderModificationRequest {

    private String orderId;
    private String userId;
    private Integer newQuantity;
    private BigDecimal newPrice;
    private BigDecimal newStopPrice;
    private TimeInForce newTimeInForce;
    private Instant newGoodTillDate;

    public void validate() {
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID is required");
        }

        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required");
        }

        // At least one field must be provided for modification
        if (newQuantity == null && newPrice == null && newStopPrice == null
            && newTimeInForce == null && newGoodTillDate == null) {
            throw new IllegalArgumentException("At least one field must be specified for modification");
        }

        // Validate new quantity if provided
        if (newQuantity != null && newQuantity <= 0) {
            throw new IllegalArgumentException("New quantity must be positive");
        }

        // Validate new price if provided
        if (newPrice != null && newPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("New price must be positive");
        }

        // Validate new stop price if provided
        if (newStopPrice != null && newStopPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("New stop price must be positive");
        }

        // Validate GTD requires expiration date
        if (newTimeInForce == TimeInForce.GTD && newGoodTillDate == null) {
            throw new IllegalArgumentException("Good till date is required for GTD orders");
        }
    }

    public boolean hasQuantityChange() {
        return newQuantity != null;
    }

    public boolean hasPriceChange() {
        return newPrice != null;
    }

    public boolean hasStopPriceChange() {
        return newStopPrice != null;
    }

    public boolean hasTimeInForceChange() {
        return newTimeInForce != null;
    }
}