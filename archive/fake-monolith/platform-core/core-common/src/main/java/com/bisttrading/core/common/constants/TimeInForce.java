package com.bisttrading.core.common.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Time in Force specifications for trading orders.
 * Defines how long an order remains active in the market.
 */
@Getter
@RequiredArgsConstructor
public enum TimeInForce {

    /**
     * Good Till Cancelled - Order remains active until explicitly cancelled
     */
    GTC("GTC", "İptal Edilene Kadar Geçerli", "Emir açıkça iptal edilene kadar aktif kalır", false, false),

    /**
     * Immediate or Cancel - Execute immediately, cancel remainder
     */
    IOC("IOC", "Anında Ya Da İptal", "Anında gerçekleşen kısmı alınır, kalan iptal edilir", true, false),

    /**
     * Fill or Kill - Execute completely and immediately or cancel
     */
    FOK("FOK", "Gerçekleş Ya Da İptal", "Tamamen ve anında gerçekleşmediği takdirde iptal edilir", true, true),

    /**
     * Good Till Date - Order remains active until specified date
     */
    GTD("GTD", "Belirtilen Tarihe Kadar Geçerli", "Belirtilen tarihe kadar aktif kalır", false, false),

    /**
     * Good for Day - Order remains active until end of trading day
     */
    DAY("DAY", "Gün İçi Geçerli", "İşlem günü sonuna kadar aktif kalır", false, false),

    /**
     * At the Opening - Execute at market opening or cancel
     */
    OPG("OPG", "Açılışta", "Piyasa açılışında gerçekleştirilir veya iptal edilir", true, false),

    /**
     * At the Close - Execute at market close or cancel
     */
    CLS("CLS", "Kapanışta", "Piyasa kapanışında gerçekleştirilir veya iptal edilir", true, false),

    /**
     * Good Till Week - Order remains active until end of week
     */
    GTW("GTW", "Hafta Sonuna Kadar", "Hafta sonuna kadar aktif kalır", false, false),

    /**
     * Good Till Month - Order remains active until end of month
     */
    GTM("GTM", "Ay Sonuna Kadar", "Ay sonuna kadar aktif kalır", false, false);

    private final String code;
    private final String displayName;
    private final String description;
    private final boolean immediateExecution;
    private final boolean requiresCompleteExecution;

    /**
     * Checks if this time in force requires immediate execution.
     *
     * @return true if immediate execution is required
     */
    public boolean isImmediateExecution() {
        return this.immediateExecution;
    }

    /**
     * Checks if this time in force requires complete execution.
     *
     * @return true if complete execution is required
     */
    public boolean requiresCompleteExecution() {
        return this.requiresCompleteExecution;
    }

    /**
     * Checks if this time in force allows partial fills.
     *
     * @return true if partial fills are allowed
     */
    public boolean allowsPartialFills() {
        return !this.requiresCompleteExecution;
    }

    /**
     * Checks if this time in force requires an expiration date.
     *
     * @return true if expiration date is required
     */
    public boolean requiresExpirationDate() {
        return this == GTD;
    }

    /**
     * Checks if this time in force is compatible with the given order type.
     *
     * @param orderType The order type to check compatibility with
     * @return true if compatible
     */
    public boolean isCompatibleWith(OrderType orderType) {
        // FOK and IOC are not compatible with market orders
        if (orderType == OrderType.MARKET && (this == FOK || this == IOC)) {
            return false;
        }

        // OPG and CLS are only compatible with limit orders
        if ((this == OPG || this == CLS) && orderType != OrderType.LIMIT) {
            return false;
        }

        // All or None orders are compatible with GTC and GTD
        if (orderType == OrderType.ALL_OR_NONE && (this == IOC || this == FOK)) {
            return false;
        }

        return true;
    }

    /**
     * Checks if this time in force is compatible with the given market.
     *
     * @param market The market to check compatibility with
     * @return true if compatible
     */
    public boolean isCompatibleWith(Markets market) {
        // All time in force types are compatible with BIST and VIOP
        if (market == Markets.BIST || market == Markets.VIOP) {
            return true;
        }

        // FOREX and CRYPTO markets typically support fewer TIF types
        if (market == Markets.FOREX || market == Markets.CRYPTO) {
            return this == GTC || this == IOC || this == FOK || this == DAY;
        }

        return true;
    }

    /**
     * Finds time in force by code.
     *
     * @param code Time in force code
     * @return TimeInForce enum or null if not found
     */
    public static TimeInForce findByCode(String code) {
        for (TimeInForce tif : values()) {
            if (tif.getCode().equalsIgnoreCase(code)) {
                return tif;
            }
        }
        return null;
    }

    /**
     * Returns time in force types that require immediate execution.
     *
     * @return Array of immediate execution TIF types
     */
    public static TimeInForce[] getImmediateExecutionTypes() {
        return new TimeInForce[]{IOC, FOK, OPG, CLS};
    }

    /**
     * Returns time in force types that allow partial fills.
     *
     * @return Array of partial-fill-allowed TIF types
     */
    public static TimeInForce[] getPartialFillAllowedTypes() {
        return new TimeInForce[]{GTC, IOC, GTD, DAY, OPG, CLS, GTW, GTM};
    }

    /**
     * Returns time in force types suitable for the given market.
     *
     * @param market The market
     * @return Array of compatible time in force types
     */
    public static TimeInForce[] getCompatibleTypes(Markets market) {
        return java.util.Arrays.stream(values())
                .filter(tif -> tif.isCompatibleWith(market))
                .toArray(TimeInForce[]::new);
    }

    /**
     * Returns time in force types compatible with the given order type.
     *
     * @param orderType The order type
     * @return Array of compatible time in force types
     */
    public static TimeInForce[] getCompatibleTypes(OrderType orderType) {
        return java.util.Arrays.stream(values())
                .filter(tif -> tif.isCompatibleWith(orderType))
                .toArray(TimeInForce[]::new);
    }
}