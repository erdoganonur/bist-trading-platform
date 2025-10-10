package com.bisttrading.trading.exception;

/**
 * Exception thrown when a symbol cannot be found.
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since 2.0.0
 */
public class SymbolNotFoundException extends RuntimeException {

    private final String symbol;

    public SymbolNotFoundException(String symbol) {
        super(String.format("Symbol not found: %s", symbol));
        this.symbol = symbol;
    }

    public SymbolNotFoundException(String symbol, String message) {
        super(message);
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
