package com.bisttrading.core.domain.valueobjects;

import com.bisttrading.core.common.exceptions.ValidationException;
import com.bisttrading.core.common.constants.ErrorCodes;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Symbol value object representing a trading symbol with exchange and market information.
 * Immutable and provides symbol validation and formatting.
 */
@Getter
@EqualsAndHashCode
public final class Symbol {

    private static final Pattern SYMBOL_PATTERN = Pattern.compile("^[A-Z0-9]{3,10}$");
    private static final Pattern ISIN_PATTERN = Pattern.compile("^[A-Z]{2}[A-Z0-9]{9}[0-9]$");

    private final String code;
    private final String isin;
    private final String companyName;
    private final Exchange exchange;
    private final MarketType marketType;
    private final boolean isActive;

    private Symbol(String code, String isin, String companyName, Exchange exchange, MarketType marketType, boolean isActive) {
        this.code = Objects.requireNonNull(code, "Sembol kodu boş olamaz").trim().toUpperCase();
        this.isin = isin != null ? isin.trim().toUpperCase() : null;
        this.companyName = Objects.requireNonNull(companyName, "Şirket adı boş olamaz").trim();
        this.exchange = Objects.requireNonNull(exchange, "Borsa boş olamaz");
        this.marketType = Objects.requireNonNull(marketType, "Pazar türü boş olamaz");
        this.isActive = isActive;

        validateSymbolCode(this.code);
        validateIsin(this.isin);
        validateCompanyName(this.companyName);
    }

    /**
     * Creates a Symbol instance.
     *
     * @param code        The symbol code (e.g., "GARAN", "THYAO")
     * @param isin        The ISIN code
     * @param companyName The company name
     * @param exchange    The exchange
     * @param marketType  The market type
     * @param isActive    Whether the symbol is active for trading
     * @return Symbol instance
     */
    public static Symbol of(String code, String isin, String companyName, Exchange exchange, MarketType marketType, boolean isActive) {
        return new Symbol(code, isin, companyName, exchange, marketType, isActive);
    }

    /**
     * Creates an active Symbol instance.
     *
     * @param code        The symbol code
     * @param isin        The ISIN code
     * @param companyName The company name
     * @param exchange    The exchange
     * @param marketType  The market type
     * @return Active Symbol instance
     */
    public static Symbol of(String code, String isin, String companyName, Exchange exchange, MarketType marketType) {
        return new Symbol(code, isin, companyName, exchange, marketType, true);
    }

    /**
     * Creates a BIST Symbol instance.
     *
     * @param code        The symbol code
     * @param isin        The ISIN code
     * @param companyName The company name
     * @param marketType  The market type
     * @return BIST Symbol instance
     */
    public static Symbol bistSymbol(String code, String isin, String companyName, MarketType marketType) {
        return new Symbol(code, isin, companyName, Exchange.BIST, marketType, true);
    }

    /**
     * Creates a simple BIST Symbol for main market.
     *
     * @param code        The symbol code
     * @param companyName The company name
     * @return Simple BIST Symbol instance
     */
    public static Symbol bistSymbol(String code, String companyName) {
        return new Symbol(code, null, companyName, Exchange.BIST, MarketType.ANA_PAZAR, true);
    }

    /**
     * Gets the full symbol identifier including exchange.
     *
     * @return Full symbol identifier (e.g., "BIST:GARAN")
     */
    public String getFullSymbol() {
        return exchange.getCode() + ":" + code;
    }

    /**
     * Gets the display name combining code and company name.
     *
     * @return Display name (e.g., "GARAN - Türkiye Garanti Bankası A.Ş.")
     */
    public String getDisplayName() {
        return code + " - " + companyName;
    }

    /**
     * Checks if this symbol is traded on BIST.
     *
     * @return true if BIST symbol
     */
    public boolean isBistSymbol() {
        return exchange == Exchange.BIST;
    }

    /**
     * Checks if this symbol is traded on a foreign exchange.
     *
     * @return true if foreign symbol
     */
    public boolean isForeignSymbol() {
        return exchange != Exchange.BIST;
    }

    /**
     * Checks if this symbol belongs to main market.
     *
     * @return true if main market symbol
     */
    public boolean isMainMarketSymbol() {
        return marketType.isMainMarket();
    }

    /**
     * Checks if this symbol requires special trading rules.
     *
     * @return true if special rules apply
     */
    public boolean hasSpecialTradingRules() {
        return marketType.hasSpecialTradingRules();
    }

    /**
     * Checks if this symbol is valid for trading.
     *
     * @return true if valid for trading
     */
    public boolean isValidForTrading() {
        return isActive && exchange != null && marketType != null;
    }

    /**
     * Checks if this symbol supports derivatives trading.
     *
     * @return true if derivatives are supported
     */
    public boolean supportsDerivatives() {
        return isBistSymbol() &&
               (marketType == MarketType.YILDIZ_PAZAR || marketType == MarketType.ANA_PAZAR);
    }

    /**
     * Gets the minimum lot size for this symbol based on market type.
     *
     * @return Minimum lot size
     */
    public int getMinimumLotSize() {
        return switch (marketType) {
            case YILDIZ_PAZAR -> 1;
            case ANA_PAZAR -> 1;
            case GELISIM_PAZARI -> 10;
            case KOLEKTIF_YATIRIM -> 1;
            case YAPILANDIRILMIS_URUNLER -> 100;
        };
    }

    /**
     * Gets the tick size for this symbol based on market type.
     *
     * @return Tick size as string
     */
    public String getTickSize() {
        return switch (marketType) {
            case YILDIZ_PAZAR, ANA_PAZAR -> "0.01";
            case GELISIM_PAZARI -> "0.01";
            case KOLEKTIF_YATIRIM -> "0.001";
            case YAPILANDIRILMIS_URUNLER -> "0.0001";
        };
    }

    /**
     * Gets the daily limit percentage for this symbol.
     *
     * @return Daily limit percentage
     */
    public double getDailyLimitPercentage() {
        return switch (marketType) {
            case YILDIZ_PAZAR, ANA_PAZAR -> 10.0;
            case GELISIM_PAZARI -> 20.0;
            case KOLEKTIF_YATIRIM -> 10.0;
            case YAPILANDIRILMIS_URUNLER -> 30.0;
        };
    }

    /**
     * Creates a trading session identifier for this symbol.
     *
     * @return Trading session identifier
     */
    public String getTradingSessionId() {
        return String.format("%s_%s_%s", exchange.getCode(), marketType.getCode(), code);
    }

    /**
     * Checks if this symbol equals another symbol (by code and exchange).
     *
     * @param other The other symbol
     * @return true if symbols are equal
     */
    public boolean isSameSymbol(Symbol other) {
        if (other == null) {
            return false;
        }
        return this.code.equals(other.code) && this.exchange.equals(other.exchange);
    }

    /**
     * Formats this symbol for logging.
     *
     * @return Formatted string for logging
     */
    public String formatForLogging() {
        return String.format("[%s:%s:%s]", exchange.getCode(), marketType.getCode(), code);
    }

    /**
     * Validates symbol code format.
     */
    private void validateSymbolCode(String symbolCode) {
        if (symbolCode.isEmpty()) {
            throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                "Sembol kodu boş olamaz");
        }

        if (!SYMBOL_PATTERN.matcher(symbolCode).matches()) {
            throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                "Sembol kodu 3-10 karakter arası büyük harf ve rakam içermelidir: " + symbolCode);
        }

        // BIST specific validations
        if (exchange == Exchange.BIST) {
            if (symbolCode.length() < 4 || symbolCode.length() > 6) {
                throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                    "BIST sembol kodu 4-6 karakter arasında olmalıdır: " + symbolCode);
            }
        }
    }

    /**
     * Validates ISIN code format.
     */
    private void validateIsin(String isinCode) {
        if (isinCode != null && !isinCode.isEmpty()) {
            if (!ISIN_PATTERN.matcher(isinCode).matches()) {
                throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                    "Geçersiz ISIN kodu formatı: " + isinCode);
            }

            // BIST specific ISIN validation
            if (exchange == Exchange.BIST && !isinCode.startsWith("TR")) {
                throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                    "BIST sembolleri için ISIN kodu 'TR' ile başlamalıdır: " + isinCode);
            }
        }
    }

    /**
     * Validates company name.
     */
    private void validateCompanyName(String name) {
        if (name.isEmpty()) {
            throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                "Şirket adı boş olamaz");
        }

        if (name.length() > 100) {
            throw new ValidationException(ErrorCodes.VALIDATION_ERROR,
                "Şirket adı 100 karakterden uzun olamaz");
        }
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    /**
     * Builder for Symbol instances.
     */
    public static class Builder {
        private String code;
        private String isin;
        private String companyName;
        private Exchange exchange = Exchange.BIST;
        private MarketType marketType = MarketType.ANA_PAZAR;
        private boolean isActive = true;

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder isin(String isin) {
            this.isin = isin;
            return this;
        }

        public Builder companyName(String companyName) {
            this.companyName = companyName;
            return this;
        }

        public Builder exchange(Exchange exchange) {
            this.exchange = exchange;
            return this;
        }

        public Builder exchange(String exchangeCode) {
            this.exchange = Exchange.fromCode(exchangeCode);
            return this;
        }

        public Builder marketType(MarketType marketType) {
            this.marketType = marketType;
            return this;
        }

        public Builder marketType(String marketTypeCode) {
            this.marketType = MarketType.fromCode(marketTypeCode);
            return this;
        }

        public Builder active(boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public Symbol build() {
            return Symbol.of(code, isin, companyName, exchange, marketType, isActive);
        }
    }

    /**
     * Creates a new Builder instance.
     *
     * @return Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}