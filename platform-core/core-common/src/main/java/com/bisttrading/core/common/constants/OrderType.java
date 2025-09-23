package com.bisttrading.core.common.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Order types supported by the trading platform.
 */
@Getter
@RequiredArgsConstructor
public enum OrderType {

    /**
     * Market order - executed immediately at best available price
     */
    MARKET("MARKET", "Piyasa Emri", "En iyi fiyattan anında gerçekleştirilen emir", false, true),

    /**
     * Limit order - executed only at specified price or better
     */
    LIMIT("LIMIT", "Limitli Emir", "Belirtilen fiyattan veya daha iyisinden gerçekleştirilen emir", true, false),

    /**
     * Stop order - becomes market order when stop price is reached
     */
    STOP("STOP", "Stop Emir", "Stop fiyatına ulaştığında piyasa emrine dönüşen emir", true, true),

    /**
     * Stop limit order - becomes limit order when stop price is reached
     */
    STOP_LIMIT("STOP_LIMIT", "Stop Limitli Emir", "Stop fiyatına ulaştığında limitli emire dönüşen emir", true, false),

    /**
     * Iceberg order - large order split into smaller visible portions
     */
    ICEBERG("ICEBERG", "Buzdağı Emir", "Büyük emrin küçük parçalar halinde gösterildiği emir", true, false),

    /**
     * All or None order - must be executed completely or not at all
     */
    ALL_OR_NONE("ALL_OR_NONE", "Hep Ya Hiç", "Tamamen gerçekleşmediği takdirde iptal olan emir", true, false),

    /**
     * Fill or Kill order - must be executed immediately and completely
     */
    FILL_OR_KILL("FILL_OR_KILL", "Gerçekleş Ya Da İptal", "Anında tamamen gerçekleşmediği takdirde iptal olan emir", true, true),

    /**
     * Immediate or Cancel order - execute immediately, cancel remainder
     */
    IMMEDIATE_OR_CANCEL("IMMEDIATE_OR_CANCEL", "Anında Ya Da İptal", "Anında gerçekleşen kısmı alınır, kalan iptal edilir", true, true);

    private final String code;
    private final String displayName;
    private final String description;
    private final boolean requiresPrice;
    private final boolean immediateExecution;

    /**
     * Checks if this order type requires a limit price.
     *
     * @return true if price is required
     */
    public boolean isPriceRequired() {
        return this.requiresPrice;
    }

    /**
     * Checks if this order type requires a stop price.
     *
     * @return true if stop price is required
     */
    public boolean isStopPriceRequired() {
        return this == STOP || this == STOP_LIMIT;
    }

    /**
     * Checks if this order type executes immediately.
     *
     * @return true if immediate execution
     */
    public boolean isImmediateExecution() {
        return this.immediateExecution;
    }

    /**
     * Checks if this order type can be partially filled.
     *
     * @return true if partial fills are allowed
     */
    public boolean allowsPartialFills() {
        return this != ALL_OR_NONE && this != FILL_OR_KILL;
    }

    /**
     * Checks if this order type is compatible with the given market.
     *
     * @param market The market to check compatibility with
     * @return true if compatible
     */
    public boolean isCompatibleWith(Markets market) {
        // All order types are compatible with BIST
        if (market == Markets.BIST || market == Markets.VIOP) {
            return true;
        }

        // FOREX and CRYPTO markets typically don't support all order types
        if (market == Markets.FOREX || market == Markets.CRYPTO) {
            return this == MARKET || this == LIMIT || this == STOP || this == STOP_LIMIT;
        }

        return true;
    }

    /**
     * Finds order type by code.
     *
     * @param code Order type code
     * @return OrderType enum or null if not found
     */
    public static OrderType findByCode(String code) {
        for (OrderType orderType : values()) {
            if (orderType.getCode().equalsIgnoreCase(code)) {
                return orderType;
            }
        }
        return null;
    }

    /**
     * Returns order types that require immediate execution.
     *
     * @return Array of immediate execution order types
     */
    public static OrderType[] getImmediateExecutionTypes() {
        return new OrderType[]{MARKET, STOP, FILL_OR_KILL, IMMEDIATE_OR_CANCEL};
    }

    /**
     * Returns order types that require a limit price.
     *
     * @return Array of price-required order types
     */
    public static OrderType[] getPriceRequiredTypes() {
        return new OrderType[]{LIMIT, STOP, STOP_LIMIT, ICEBERG, ALL_OR_NONE, FILL_OR_KILL, IMMEDIATE_OR_CANCEL};
    }

    /**
     * Returns order types suitable for the given market.
     *
     * @param market The market
     * @return Array of compatible order types
     */
    public static OrderType[] getCompatibleTypes(Markets market) {
        return java.util.Arrays.stream(values())
                .filter(type -> type.isCompatibleWith(market))
                .toArray(OrderType[]::new);
    }
}