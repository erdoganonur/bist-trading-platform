package com.bisttrading.broker.algolab.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Order side enumeration for AlgoLab API
 */
public enum OrderSide {
    BUY("BUY"),
    SELL("SELL");

    private final String value;

    OrderSide(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static OrderSide fromValue(String value) {
        if (value == null) {
            return null;
        }

        for (OrderSide side : OrderSide.values()) {
            if (side.value.equalsIgnoreCase(value)) {
                return side;
            }
        }

        throw new IllegalArgumentException("Unknown order side: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}