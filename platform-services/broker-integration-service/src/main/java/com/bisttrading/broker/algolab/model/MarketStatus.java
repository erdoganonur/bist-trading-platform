package com.bisttrading.broker.algolab.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MarketStatus {
    OPEN("OPEN"),
    CLOSED("CLOSED"),
    PRE_MARKET("PRE_MARKET"),
    POST_MARKET("POST_MARKET"),
    AUCTION("AUCTION"),
    SUSPENDED("SUSPENDED"),
    HALT("HALT");

    private final String value;

    MarketStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static MarketStatus fromValue(String value) {
        if (value == null) {
            return null;
        }

        for (MarketStatus status : MarketStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unknown market status: " + value);
    }

    public boolean allowsTrading() {
        return this == OPEN || this == AUCTION;
    }

    public boolean isActive() {
        return this == OPEN || this == PRE_MARKET || this == POST_MARKET || this == AUCTION;
    }

    public String getDescription() {
        return switch (this) {
            case OPEN -> "Market is open for continuous trading";
            case CLOSED -> "Market is closed";
            case PRE_MARKET -> "Pre-market trading session";
            case POST_MARKET -> "Post-market trading session";
            case AUCTION -> "Market is in auction mode";
            case SUSPENDED -> "Market trading is suspended";
            case HALT -> "Market trading is halted";
        };
    }

    @Override
    public String toString() {
        return value;
    }
}