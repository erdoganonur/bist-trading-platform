package com.bisttrading.entity.trading.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Enumeration for different market types in the trading platform
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since Sprint 3
 */
@Getter
public enum MarketType {
    EQUITY("EQUITY", "Stock Market", "Hisse Senedi Piyasası"),
    BOND("BOND", "Bond Market", "Tahvil Piyasası"),
    DERIVATIVE("DERIVATIVE", "Derivatives Market", "Türev Araçlar Piyasası"),
    CURRENCY("CURRENCY", "Currency Market", "Döviz Piyasası"),
    COMMODITY("COMMODITY", "Commodity Market", "Emtia Piyasası"),
    CRYPTO("CRYPTO", "Cryptocurrency Market", "Kripto Para Piyasası");

    private final String code;
    private final String description;
    private final String turkishDescription;

    MarketType(String code, String description, String turkishDescription) {
        this.code = code;
        this.description = description;
        this.turkishDescription = turkishDescription;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public static MarketType fromCode(String code) {
        for (MarketType marketType : values()) {
            if (marketType.code.equalsIgnoreCase(code)) {
                return marketType;
            }
        }
        throw new IllegalArgumentException("Unknown market type code: " + code);
    }

    @Override
    public String toString() {
        return code;
    }
}