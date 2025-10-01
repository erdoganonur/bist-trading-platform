package com.bisttrading.broker.algolab.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Order type enumeration for AlgoLab API
 */
public enum OrderType {
    MARKET("MARKET"),
    LIMIT("LIMIT"),
    STOP("STOP"),
    STOP_LIMIT("STOP_LIMIT"),
    TRAILING_STOP("TRAILING_STOP"),
    ICEBERG("ICEBERG");

    private final String value;

    OrderType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static OrderType fromValue(String value) {
        if (value == null) {
            return null;
        }

        for (OrderType type : OrderType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unknown order type: " + value);
    }

    /**
     * Check if order type requires price
     */
    public boolean requiresPrice() {
        return this == LIMIT || this == STOP_LIMIT;
    }

    /**
     * Check if order type requires stop price
     */
    public boolean requiresStopPrice() {
        return this == STOP || this == STOP_LIMIT || this == TRAILING_STOP;
    }

    /**
     * Check if order type is market order
     */
    public boolean isMarketOrder() {
        return this == MARKET;
    }

    /**
     * Check if order type supports time in force
     */
    public boolean supportsTimeInForce() {
        return this != MARKET; // Market orders are immediate
    }

    @Override
    public String toString() {
        return value;
    }
}