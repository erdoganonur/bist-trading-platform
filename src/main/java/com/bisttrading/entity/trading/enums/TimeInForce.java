package com.bisttrading.entity.trading.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Enumeration for Time In Force specifications
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since Sprint 3
 */
@Getter
public enum TimeInForce {
    DAY("DAY", "Day Order", "Günlük Emir", "Valid for current trading day only"),
    GTC("GTC", "Good Till Cancelled", "İptal Edilene Kadar", "Valid until manually cancelled"),
    IOC("IOC", "Immediate or Cancel", "Hemen veya İptal", "Execute immediately, cancel unfilled portion"),
    FOK("FOK", "Fill or Kill", "Tamamen veya İptal", "Execute completely or cancel entirely"),
    GTD("GTD", "Good Till Date", "Belirli Tarihe Kadar", "Valid until specified date"),
    OPG("OPG", "At the Opening", "Açılışta", "Execute at market opening only"),
    CLS("CLS", "At the Close", "Kapanışta", "Execute at market close only");

    private final String code;
    private final String description;
    private final String turkishDescription;
    private final String explanation;

    TimeInForce(String code, String description, String turkishDescription, String explanation) {
        this.code = code;
        this.description = description;
        this.turkishDescription = turkishDescription;
        this.explanation = explanation;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public static TimeInForce fromCode(String code) {
        for (TimeInForce tif : values()) {
            if (tif.code.equalsIgnoreCase(code)) {
                return tif;
            }
        }
        throw new IllegalArgumentException("Unknown time in force code: " + code);
    }

    /**
     * Check if this TIF requires immediate execution
     */
    public boolean requiresImmediateExecution() {
        return this == IOC || this == FOK;
    }

    /**
     * Check if this TIF allows partial fills
     */
    public boolean allowsPartialFills() {
        return this != FOK;
    }

    /**
     * Check if this TIF requires an expiry date
     */
    public boolean requiresExpiryDate() {
        return this == GTD;
    }

    /**
     * Check if this TIF is session-specific
     */
    public boolean isSessionSpecific() {
        return this == OPG || this == CLS;
    }

    @Override
    public String toString() {
        return code;
    }
}