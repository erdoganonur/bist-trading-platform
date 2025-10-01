package com.bisttrading.core.common.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Order status enumeration for trading operations.
 */
@Getter
@RequiredArgsConstructor
public enum OrderStatus {

    /**
     * Order is created but not yet sent to market
     */
    PENDING("PENDING", "Beklemede", "Emir oluşturuldu ancak henüz piyasaya gönderilmedi"),

    /**
     * Order has been sent to the market
     */
    SENT("SENT", "Gönderildi", "Emir piyasaya gönderildi"),

    /**
     * Order is active in the market
     */
    ACTIVE("ACTIVE", "Aktif", "Emir piyasada aktif durumda"),

    /**
     * Order has been partially filled
     */
    PARTIALLY_FILLED("PARTIALLY_FILLED", "Kısmi Gerçekleşti", "Emir kısmen gerçekleştirildi"),

    /**
     * Order has been completely filled
     */
    FILLED("FILLED", "Gerçekleşti", "Emir tamamen gerçekleştirildi"),

    /**
     * Order has been cancelled by user
     */
    CANCELLED("CANCELLED", "İptal Edildi", "Emir kullanıcı tarafından iptal edildi"),

    /**
     * Order has been rejected by the market
     */
    REJECTED("REJECTED", "Reddedildi", "Emir piyasa tarafından reddedildi"),

    /**
     * Order has expired (for time-limited orders)
     */
    EXPIRED("EXPIRED", "Süresi Doldu", "Emir süresi doldu"),

    /**
     * Order is being cancelled
     */
    PENDING_CANCEL("PENDING_CANCEL", "İptal Beklemede", "Emir iptal işlemi beklemede"),

    /**
     * Order cancellation was rejected
     */
    CANCEL_REJECTED("CANCEL_REJECTED", "İptal Reddedildi", "Emir iptal işlemi reddedildi"),

    /**
     * Order is being modified
     */
    PENDING_REPLACE("PENDING_REPLACE", "Değişiklik Beklemede", "Emir değişiklik işlemi beklemede"),

    /**
     * Order modification was rejected
     */
    REPLACE_REJECTED("REPLACE_REJECTED", "Değişiklik Reddedildi", "Emir değişiklik işlemi reddedildi"),

    /**
     * Order is suspended
     */
    SUSPENDED("SUSPENDED", "Askıya Alındı", "Emir askıya alındı"),

    /**
     * System error occurred
     */
    ERROR("ERROR", "Hata", "Sistem hatası oluştu");

    private final String code;
    private final String displayName;
    private final String description;

    /**
     * Checks if the order is in a final state (cannot be modified or cancelled).
     *
     * @return true if order is in final state
     */
    public boolean isFinalState() {
        return this == FILLED || this == CANCELLED || this == REJECTED ||
               this == EXPIRED || this == ERROR;
    }

    /**
     * Checks if the order is in an active state (can be cancelled or modified).
     *
     * @return true if order is active
     */
    public boolean isActiveState() {
        return this == SENT || this == ACTIVE || this == PARTIALLY_FILLED;
    }

    /**
     * Checks if the order is in a pending state.
     *
     * @return true if order is pending
     */
    public boolean isPendingState() {
        return this == PENDING || this == PENDING_CANCEL || this == PENDING_REPLACE;
    }

    /**
     * Checks if the order was executed (fully or partially).
     *
     * @return true if order was executed
     */
    public boolean isExecuted() {
        return this == FILLED || this == PARTIALLY_FILLED;
    }

    /**
     * Checks if the order can be cancelled.
     *
     * @return true if order can be cancelled
     */
    public boolean isCancellable() {
        return this == PENDING || this == SENT || this == ACTIVE || this == PARTIALLY_FILLED;
    }

    /**
     * Checks if the order can be modified.
     *
     * @return true if order can be modified
     */
    public boolean isModifiable() {
        return this == PENDING || this == SENT || this == ACTIVE;
    }

    /**
     * Finds order status by code.
     *
     * @param code Status code
     * @return OrderStatus enum or null if not found
     */
    public static OrderStatus findByCode(String code) {
        for (OrderStatus status : values()) {
            if (status.getCode().equalsIgnoreCase(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * Returns all final states.
     *
     * @return Array of final order states
     */
    public static OrderStatus[] getFinalStates() {
        return new OrderStatus[]{FILLED, CANCELLED, REJECTED, EXPIRED, ERROR};
    }

    /**
     * Returns all active states.
     *
     * @return Array of active order states
     */
    public static OrderStatus[] getActiveStates() {
        return new OrderStatus[]{SENT, ACTIVE, PARTIALLY_FILLED};
    }
}