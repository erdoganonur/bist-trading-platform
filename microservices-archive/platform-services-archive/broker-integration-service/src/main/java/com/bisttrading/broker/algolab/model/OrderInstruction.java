package com.bisttrading.broker.algolab.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Order instruction enumeration for position management
 */
public enum OrderInstruction {
    OPEN("OPEN"),         // Open new position
    CLOSE("CLOSE"),       // Close existing position
    INCREASE("INCREASE"), // Increase existing position
    DECREASE("DECREASE"); // Decrease existing position

    private final String value;

    OrderInstruction(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static OrderInstruction fromValue(String value) {
        if (value == null) {
            return null;
        }

        for (OrderInstruction instruction : OrderInstruction.values()) {
            if (instruction.value.equalsIgnoreCase(value)) {
                return instruction;
            }
        }

        throw new IllegalArgumentException("Unknown order instruction: " + value);
    }

    /**
     * Check if instruction opens a position
     */
    public boolean opensPosition() {
        return this == OPEN || this == INCREASE;
    }

    /**
     * Check if instruction closes a position
     */
    public boolean closesPosition() {
        return this == CLOSE || this == DECREASE;
    }

    /**
     * Get description of the instruction
     */
    public String getDescription() {
        return switch (this) {
            case OPEN -> "Open a new position";
            case CLOSE -> "Close an existing position";
            case INCREASE -> "Increase existing position size";
            case DECREASE -> "Decrease existing position size";
        };
    }

    @Override
    public String toString() {
        return value;
    }
}