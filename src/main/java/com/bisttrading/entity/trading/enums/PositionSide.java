package com.bisttrading.entity.trading.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Enumeration for position side (long/short/flat)
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since Sprint 3
 */
@Getter
public enum PositionSide {
    LONG("LONG", "Long Position", "Uzun Pozisyon", 1),
    SHORT("SHORT", "Short Position", "Kısa Pozisyon", -1),
    FLAT("FLAT", "No Position", "Pozisyon Yok", 0);

    private final String code;
    private final String description;
    private final String turkishDescription;
    private final int direction; // +1 for long, -1 for short, 0 for flat

    PositionSide(String code, String description, String turkishDescription, int direction) {
        this.code = code;
        this.description = description;
        this.turkishDescription = turkishDescription;
        this.direction = direction;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public static PositionSide fromCode(String code) {
        for (PositionSide side : values()) {
            if (side.code.equalsIgnoreCase(code)) {
                return side;
            }
        }
        throw new IllegalArgumentException("Unknown position side code: " + code);
    }

    /**
     * Determine position side based on quantity
     */
    public static PositionSide fromQuantity(Integer quantity) {
        if (quantity == null || quantity == 0) return FLAT;
        return quantity > 0 ? LONG : SHORT;
    }

    /**
     * Check if position has risk exposure
     */
    public boolean hasExposure() {
        return this != FLAT;
    }

    /**
     * Get the closing side for this position
     */
    public OrderSide getClosingSide() {
        switch (this) {
            case LONG:
                return OrderSide.SELL;
            case SHORT:
                return OrderSide.BUY;
            default:
                throw new IllegalStateException("Cannot determine closing side for FLAT position");
        }
    }

    @Override
    public String toString() {
        return code;
    }
}