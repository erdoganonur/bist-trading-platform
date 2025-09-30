package com.bisttrading.broker.algolab.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderStatus {
    NEW("NEW"),
    PARTIALLY_FILLED("PARTIALLY_FILLED"),
    FILLED("FILLED"),
    CANCELLED("CANCELLED"),
    PENDING_CANCEL("PENDING_CANCEL"),
    REJECTED("REJECTED"),
    EXPIRED("EXPIRED"),
    SUSPENDED("SUSPENDED");

    private final String value;

    OrderStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static OrderStatus fromValue(String value) {
        if (value == null) {
            return null;
        }

        for (OrderStatus status : OrderStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unknown order status: " + value);
    }

    public boolean isActive() {
        return this == NEW || this == PARTIALLY_FILLED;
    }

    public boolean isFinal() {
        return this == FILLED || this == CANCELLED || this == REJECTED || this == EXPIRED;
    }

    public boolean canBeCancelled() {
        return this == NEW || this == PARTIALLY_FILLED;
    }

    public String getDescription() {
        return switch (this) {
            case NEW -> "Order has been accepted and is waiting for execution";
            case PARTIALLY_FILLED -> "Order has been partially executed";
            case FILLED -> "Order has been completely executed";
            case CANCELLED -> "Order has been cancelled";
            case PENDING_CANCEL -> "Order cancellation is pending";
            case REJECTED -> "Order has been rejected";
            case EXPIRED -> "Order has expired";
            case SUSPENDED -> "Order has been suspended";
        };
    }

    @Override
    public String toString() {
        return value;
    }
}