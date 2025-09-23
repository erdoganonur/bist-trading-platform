package com.bisttrading.core.domain.valueobjects;

import lombok.Getter;

/**
 * Market type enumeration for different trading markets.
 * Represents different market segments in BIST.
 */
@Getter
public enum MarketType {

    YILDIZ_PAZAR("YILDIZ", "Yıldız Pazar", "Ana Pazar", 1),
    ANA_PAZAR("ANA", "Ana Pazar", "Büyük şirketler için ana pazar", 2),
    GELISIM_PAZARI("GELISIM", "Gelişim Pazarı", "Büyüme potansiyeli olan şirketler", 3),
    KOLEKTIF_YATIRIM("KOLEKTIF", "Kolektif Yatırım Kuruluşları Pazarı", "Yatırım fonları ve REIT'ler", 4),
    YAPILANDIRILMIS_URUNLER("YAPILANDIRILMIS", "Yapılandırılmış Ürünler Pazarı", "Türev ürünler", 5);

    private final String code;
    private final String name;
    private final String description;
    private final int priority;

    MarketType(String code, String name, String description, int priority) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.priority = priority;
    }

    /**
     * Gets market type by code.
     *
     * @param code Market type code
     * @return MarketType enum
     * @throws IllegalArgumentException if market type not found
     */
    public static MarketType fromCode(String code) {
        for (MarketType marketType : values()) {
            if (marketType.code.equalsIgnoreCase(code)) {
                return marketType;
            }
        }
        throw new IllegalArgumentException("Desteklenmeyen pazar türü: " + code);
    }

    /**
     * Checks if this is the main market (Yıldız Pazar or Ana Pazar).
     *
     * @return true if main market
     */
    public boolean isMainMarket() {
        return this == YILDIZ_PAZAR || this == ANA_PAZAR;
    }

    /**
     * Checks if this market requires special trading rules.
     *
     * @return true if special rules apply
     */
    public boolean hasSpecialTradingRules() {
        return this == GELISIM_PAZARI || this == YAPILANDIRILMIS_URUNLER;
    }

    /**
     * Gets the trading session length in hours for this market type.
     *
     * @return Trading session length
     */
    public int getTradingSessionHours() {
        return switch (this) {
            case YILDIZ_PAZAR, ANA_PAZAR -> 8;
            case GELISIM_PAZARI -> 7;
            case KOLEKTIF_YATIRIM -> 6;
            case YAPILANDIRILMIS_URUNLER -> 10;
        };
    }
}