package com.bisttrading.core.domain.valueobjects;

import lombok.Getter;

/**
 * Currency enumeration for trading operations.
 * Represents supported currencies in the BIST Trading Platform.
 */
@Getter
public enum Currency {

    TRY("TRY", "₺", "Türk Lirası", 2),
    USD("USD", "$", "Amerikan Doları", 2),
    EUR("EUR", "€", "Euro", 2);

    private final String code;
    private final String symbol;
    private final String displayName;
    private final int decimalPlaces;

    Currency(String code, String symbol, String displayName, int decimalPlaces) {
        this.code = code;
        this.symbol = symbol;
        this.displayName = displayName;
        this.decimalPlaces = decimalPlaces;
    }

    /**
     * Gets currency by code.
     *
     * @param code Currency code
     * @return Currency enum
     * @throws IllegalArgumentException if currency not found
     */
    public static Currency fromCode(String code) {
        for (Currency currency : values()) {
            if (currency.code.equalsIgnoreCase(code)) {
                return currency;
            }
        }
        throw new IllegalArgumentException("Desteklenmeyen para birimi: " + code);
    }

    /**
     * Checks if this currency is the base currency (TRY).
     *
     * @return true if TRY
     */
    public boolean isBaseCurrency() {
        return this == TRY;
    }

    /**
     * Checks if this currency is a foreign currency.
     *
     * @return true if not TRY
     */
    public boolean isForeignCurrency() {
        return this != TRY;
    }
}