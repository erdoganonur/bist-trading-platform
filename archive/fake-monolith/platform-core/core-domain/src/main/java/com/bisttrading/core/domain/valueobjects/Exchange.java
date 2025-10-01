package com.bisttrading.core.domain.valueobjects;

import lombok.Getter;

/**
 * Exchange enumeration for trading venues.
 * Represents supported exchanges in the BIST Trading Platform.
 */
@Getter
public enum Exchange {

    BIST("BIST", "Borsa İstanbul", "TR", "Turkey"),
    NASDAQ("NASDAQ", "NASDAQ Stock Market", "US", "United States"),
    NYSE("NYSE", "New York Stock Exchange", "US", "United States");

    private final String code;
    private final String name;
    private final String countryCode;
    private final String country;

    Exchange(String code, String name, String countryCode, String country) {
        this.code = code;
        this.name = name;
        this.countryCode = countryCode;
        this.country = country;
    }

    /**
     * Gets exchange by code.
     *
     * @param code Exchange code
     * @return Exchange enum
     * @throws IllegalArgumentException if exchange not found
     */
    public static Exchange fromCode(String code) {
        for (Exchange exchange : values()) {
            if (exchange.code.equalsIgnoreCase(code)) {
                return exchange;
            }
        }
        throw new IllegalArgumentException("Desteklenmeyen borsa: " + code);
    }

    /**
     * Checks if this is the primary exchange (BIST).
     *
     * @return true if BIST
     */
    public boolean isPrimaryExchange() {
        return this == BIST;
    }

    /**
     * Checks if this is a foreign exchange.
     *
     * @return true if not BIST
     */
    public boolean isForeignExchange() {
        return this != BIST;
    }
}