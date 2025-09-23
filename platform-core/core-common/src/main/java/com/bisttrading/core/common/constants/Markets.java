package com.bisttrading.core.common.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Markets supported by the BIST Trading Platform.
 */
@Getter
@RequiredArgsConstructor
public enum Markets {

    /**
     * Borsa İstanbul Equity Market
     */
    BIST("BIST", "Borsa İstanbul", "Hisse Senedi Piyasası", "TRY", "TR"),

    /**
     * VIOP - Vadeli İşlemler ve Opsiyon Piyasası
     */
    VIOP("VIOP", "Vadeli İşlemler ve Opsiyon Piyasası", "Türev Araçlar Piyasası", "TRY", "TR"),

    /**
     * Foreign Exchange Market
     */
    FOREX("FOREX", "Döviz Piyasası", "Yabancı Para Piyasası", "USD", "GLOBAL"),

    /**
     * Cryptocurrency Market
     */
    CRYPTO("CRYPTO", "Kripto Para Piyasası", "Dijital Varlık Piyasası", "USDT", "GLOBAL"),

    /**
     * Commodity Market
     */
    COMMODITY("COMMODITY", "Emtia Piyasası", "Mal Piyasası", "USD", "GLOBAL"),

    /**
     * Bond Market
     */
    BOND("BOND", "Tahvil Piyasası", "Sabit Getirili Menkul Kıymetler", "TRY", "TR");

    private final String code;
    private final String name;
    private final String description;
    private final String baseCurrency;
    private final String region;

    /**
     * Checks if the market is a Turkish domestic market.
     *
     * @return true if Turkish domestic market
     */
    public boolean isDomestic() {
        return "TR".equals(this.region);
    }

    /**
     * Checks if the market is an international market.
     *
     * @return true if international market
     */
    public boolean isInternational() {
        return "GLOBAL".equals(this.region);
    }

    /**
     * Checks if the market supports TRY currency.
     *
     * @return true if supports TRY
     */
    public boolean supportsTRY() {
        return "TRY".equals(this.baseCurrency);
    }

    /**
     * Finds market by code.
     *
     * @param code Market code
     * @return Markets enum or null if not found
     */
    public static Markets findByCode(String code) {
        for (Markets market : values()) {
            if (market.getCode().equalsIgnoreCase(code)) {
                return market;
            }
        }
        return null;
    }

    /**
     * Returns all domestic Turkish markets.
     *
     * @return Array of domestic markets
     */
    public static Markets[] getDomesticMarkets() {
        return new Markets[]{BIST, VIOP, BOND};
    }

    /**
     * Returns all international markets.
     *
     * @return Array of international markets
     */
    public static Markets[] getInternationalMarkets() {
        return new Markets[]{FOREX, CRYPTO, COMMODITY};
    }
}