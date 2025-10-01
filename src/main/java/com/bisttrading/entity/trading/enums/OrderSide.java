package com.bisttrading.entity.trading.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Enumeration for order side (buy/sell)
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since Sprint 3
 */
@Getter
public enum OrderSide {
    BUY("BUY", "Buy", "Alış", 1),
    SELL("SELL", "Sell", "Satış", -1);

    private final String code;
    private final String description;
    private final String turkishDescription;
    private final int multiplier; // +1 for buy, -1 for sell (useful for position calculations)

    OrderSide(String code, String description, String turkishDescription, int multiplier) {
        this.code = code;
        this.description = description;
        this.turkishDescription = turkishDescription;
        this.multiplier = multiplier;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public static OrderSide fromCode(String code) {
        for (OrderSide side : values()) {
            if (side.code.equalsIgnoreCase(code)) {
                return side;
            }
        }
        throw new IllegalArgumentException("Unknown order side code: " + code);
    }

    /**
     * Get the opposite side (BUY -> SELL, SELL -> BUY)
     */
    public OrderSide getOpposite() {
        return this == BUY ? SELL : BUY;
    }

    @Override
    public String toString() {
        return code;
    }
}