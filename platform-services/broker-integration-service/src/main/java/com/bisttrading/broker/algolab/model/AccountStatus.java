package com.bisttrading.broker.algolab.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AccountStatus {
    ACTIVE("ACTIVE"),
    INACTIVE("INACTIVE"),
    SUSPENDED("SUSPENDED"),
    CLOSED("CLOSED"),
    PENDING_APPROVAL("PENDING_APPROVAL"),
    MARGIN_CALL("MARGIN_CALL"),
    RESTRICTED("RESTRICTED");

    private final String value;

    AccountStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static AccountStatus fromValue(String value) {
        if (value == null) {
            return null;
        }

        for (AccountStatus status : AccountStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unknown account status: " + value);
    }

    public boolean allowsTrading() {
        return this == ACTIVE;
    }

    public boolean requiresAttention() {
        return this == MARGIN_CALL || this == RESTRICTED || this == SUSPENDED;
    }

    public boolean isOperational() {
        return this == ACTIVE || this == MARGIN_CALL || this == RESTRICTED;
    }

    public String getDescription() {
        return switch (this) {
            case ACTIVE -> "Account is active and fully operational";
            case INACTIVE -> "Account is inactive";
            case SUSPENDED -> "Account has been suspended";
            case CLOSED -> "Account has been closed";
            case PENDING_APPROVAL -> "Account is pending approval";
            case MARGIN_CALL -> "Account has a margin call";
            case RESTRICTED -> "Account has trading restrictions";
        };
    }

    @Override
    public String toString() {
        return value;
    }
}