package com.bisttrading.user.entity.trading.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Enumeration for different order types in the trading platform
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since Sprint 3
 */
@Getter
public enum OrderType {
    MARKET("MARKET", "Market Order", "Piyasa Emri", "Execute immediately at best available price"),
    LIMIT("LIMIT", "Limit Order", "Limitli Emir", "Execute at specified price or better"),
    STOP("STOP", "Stop Order", "Stop Emir", "Becomes market order when trigger price is hit"),
    STOP_LIMIT("STOP_LIMIT", "Stop Limit Order", "Stop Limitli Emir", "Becomes limit order when trigger price is hit"),
    TRAILING_STOP("TRAILING_STOP", "Trailing Stop", "İzleyen Stop", "Stop price adjusts with favorable price movements"),
    ICEBERG("ICEBERG", "Iceberg Order", "Buzdağı Emir", "Large order with hidden quantity"),
    BRACKET("BRACKET", "Bracket Order", "Parantez Emir", "Parent order with profit target and stop loss"),
    OCO("OCO", "One-Cancels-Other", "Biri İptal Diğeri", "Two orders, execution of one cancels the other");

    private final String code;
    private final String description;
    private final String turkishDescription;
    private final String explanation;

    OrderType(String code, String description, String turkishDescription, String explanation) {
        this.code = code;
        this.description = description;
        this.turkishDescription = turkishDescription;
        this.explanation = explanation;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public static OrderType fromCode(String code) {
        for (OrderType orderType : values()) {
            if (orderType.code.equalsIgnoreCase(code)) {
                return orderType;
            }
        }
        throw new IllegalArgumentException("Unknown order type code: " + code);
    }

    /**
     * Check if this order type requires a limit price
     */
    public boolean requiresLimitPrice() {
        return this == LIMIT || this == STOP_LIMIT || this == ICEBERG;
    }

    /**
     * Check if this order type requires a stop price
     */
    public boolean requiresStopPrice() {
        return this == STOP || this == STOP_LIMIT || this == TRAILING_STOP;
    }

    /**
     * Check if this order type supports immediate execution
     */
    public boolean supportsImmediateExecution() {
        return this == MARKET || this == STOP || this == TRAILING_STOP;
    }

    /**
     * Check if this order type is complex (has child orders)
     */
    public boolean isComplexOrder() {
        return this == BRACKET || this == OCO;
    }

    @Override
    public String toString() {
        return code;
    }
}