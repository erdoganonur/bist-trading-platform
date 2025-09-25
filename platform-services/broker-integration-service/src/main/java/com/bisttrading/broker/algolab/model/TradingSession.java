package com.bisttrading.broker.algolab.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TradingSession {
    REGULAR("REGULAR"),
    PRE_MARKET("PRE_MARKET"),
    POST_MARKET("POST_MARKET"),
    EXTENDED("EXTENDED");

    private final String value;

    TradingSession(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TradingSession fromValue(String value) {
        if (value == null) {
            return null;
        }

        for (TradingSession session : TradingSession.values()) {
            if (session.value.equalsIgnoreCase(value)) {
                return session;
            }
        }

        throw new IllegalArgumentException("Unknown trading session: " + value);
    }

    public boolean isMainSession() {
        return this == REGULAR;
    }

    public boolean isExtendedHours() {
        return this == PRE_MARKET || this == POST_MARKET || this == EXTENDED;
    }

    public String getDescription() {
        return switch (this) {
            case REGULAR -> "Regular trading session";
            case PRE_MARKET -> "Pre-market trading session";
            case POST_MARKET -> "Post-market trading session";
            case EXTENDED -> "Extended hours trading session";
        };
    }

    @Override
    public String toString() {
        return value;
    }
}