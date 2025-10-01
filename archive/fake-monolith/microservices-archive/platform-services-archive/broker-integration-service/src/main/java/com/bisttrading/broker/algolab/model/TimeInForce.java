package com.bisttrading.broker.algolab.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Time in Force enumeration for AlgoLab API
 */
public enum TimeInForce {
    DAY("DAY"),           // Valid for the current trading day
    GTC("GTC"),           // Good Till Cancelled
    IOC("IOC"),           // Immediate Or Cancel
    FOK("FOK"),           // Fill Or Kill
    GTD("GTD");           // Good Till Date

    private final String value;

    TimeInForce(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TimeInForce fromValue(String value) {
        if (value == null) {
            return null;
        }

        for (TimeInForce tif : TimeInForce.values()) {
            if (tif.value.equalsIgnoreCase(value)) {
                return tif;
            }
        }

        throw new IllegalArgumentException("Unknown time in force: " + value);
    }

    /**
     * Check if TIF requires expiration date
     */
    public boolean requiresExpirationDate() {
        return this == GTD;
    }

    /**
     * Check if TIF allows partial fills
     */
    public boolean allowsPartialFills() {
        return this == DAY || this == GTC || this == GTD;
    }

    /**
     * Check if TIF is immediate execution
     */
    public boolean isImmediate() {
        return this == IOC || this == FOK;
    }

    /**
     * Get description of the time in force
     */
    public String getDescription() {
        return switch (this) {
            case DAY -> "Valid for the current trading day only";
            case GTC -> "Valid until explicitly cancelled";
            case IOC -> "Execute immediately, cancel unfilled quantity";
            case FOK -> "Execute completely or cancel entire order";
            case GTD -> "Valid until specified date";
        };
    }

    @Override
    public String toString() {
        return value;
    }
}