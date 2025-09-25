package com.bisttrading.broker.algolab.exception;

public class AlgoLabMarketDataException extends AlgoLabException {

    private final String symbol;

    public AlgoLabMarketDataException(String message) {
        super(message, "MARKET_DATA_ERROR", 503);
        this.symbol = null;
    }

    public AlgoLabMarketDataException(String message, String symbol) {
        super(message, "MARKET_DATA_ERROR", 503);
        this.symbol = symbol;
    }

    public AlgoLabMarketDataException(String message, Throwable cause) {
        super(message, "MARKET_DATA_ERROR", 503, cause);
        this.symbol = null;
    }

    public AlgoLabMarketDataException(String message, String symbol, Throwable cause) {
        super(message, "MARKET_DATA_ERROR", 503, cause);
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public boolean hasSymbol() {
        return symbol != null && !symbol.trim().isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());

        if (hasSymbol()) {
            sb.append(" [Symbol: ").append(symbol).append("]");
        }

        return sb.toString();
    }
}