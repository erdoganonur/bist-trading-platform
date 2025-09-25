package com.bisttrading.broker.algolab.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PositionSide {
    LONG("LONG"),
    SHORT("SHORT");

    private final String value;

    PositionSide(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static PositionSide fromValue(String value) {
        if (value == null) {
            return null;
        }

        for (PositionSide side : PositionSide.values()) {
            if (side.value.equalsIgnoreCase(value)) {
                return side;
            }
        }

        throw new IllegalArgumentException("Unknown position side: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}