package com.bisttrading.broker.algolab.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AccountType {
    CASH("CASH"),
    MARGIN("MARGIN"),
    PATTERN_DAY_TRADER("PATTERN_DAY_TRADER"),
    INSTITUTIONAL("INSTITUTIONAL");

    private final String value;

    AccountType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static AccountType fromValue(String value) {
        if (value == null) {
            return null;
        }

        for (AccountType type : AccountType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unknown account type: " + value);
    }

    public boolean supportsMarginTrading() {
        return this == MARGIN || this == PATTERN_DAY_TRADER || this == INSTITUTIONAL;
    }

    public boolean supportsDayTrading() {
        return this == PATTERN_DAY_TRADER || this == INSTITUTIONAL;
    }

    public boolean supportsShortSelling() {
        return supportsMarginTrading();
    }

    public String getDescription() {
        return switch (this) {
            case CASH -> "Cash account - no margin trading";
            case MARGIN -> "Margin account - allows borrowing for trading";
            case PATTERN_DAY_TRADER -> "Pattern day trader account with enhanced buying power";
            case INSTITUTIONAL -> "Institutional account with advanced features";
        };
    }

    @Override
    public String toString() {
        return value;
    }
}