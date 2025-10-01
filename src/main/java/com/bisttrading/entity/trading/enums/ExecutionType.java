package com.bisttrading.entity.trading.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Enumeration for execution report types
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since Sprint 3
 */
@Getter
public enum ExecutionType {
    NEW("NEW", "New Order", "Yeni Emir"),
    PARTIAL_FILL("PARTIAL_FILL", "Partial Fill", "Kısmi Gerçekleşme"),
    FILL("FILL", "Complete Fill", "Tam Gerçekleşme"),
    DONE_FOR_DAY("DONE_FOR_DAY", "Done For Day", "Gün İçin Tamamlandı"),
    CANCELED("CANCELED", "Canceled", "İptal Edildi"),
    REPLACED("REPLACED", "Replaced", "Değiştirildi"),
    PENDING_CANCEL("PENDING_CANCEL", "Pending Cancel", "İptal Bekleniyor"),
    STOPPED("STOPPED", "Stopped", "Durduruldu"),
    REJECTED("REJECTED", "Rejected", "Reddedildi"),
    SUSPENDED("SUSPENDED", "Suspended", "Askıya Alındı"),
    PENDING_NEW("PENDING_NEW", "Pending New", "Yeni Emir Bekleniyor"),
    CALCULATED("CALCULATED", "Calculated", "Hesaplandı"),
    EXPIRED("EXPIRED", "Expired", "Süresi Doldu"),
    RESTATED("RESTATED", "Restated", "Yeniden Belirtildi"),
    PENDING_REPLACE("PENDING_REPLACE", "Pending Replace", "Değişiklik Bekleniyor"),
    TRADE("TRADE", "Trade Execution", "İşlem Gerçekleşme"),
    TRADE_CORRECT("TRADE_CORRECT", "Trade Correction", "İşlem Düzeltme"),
    TRADE_CANCEL("TRADE_CANCEL", "Trade Cancellation", "İşlem İptali"),
    ORDER_STATUS("ORDER_STATUS", "Order Status Update", "Emir Durum Güncelleme");

    private final String code;
    private final String description;
    private final String turkishDescription;

    ExecutionType(String code, String description, String turkishDescription) {
        this.code = code;
        this.description = description;
        this.turkishDescription = turkishDescription;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public static ExecutionType fromCode(String code) {
        for (ExecutionType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown execution type code: " + code);
    }

    /**
     * Check if this execution type represents a trade
     */
    public boolean isTrade() {
        return this == FILL || this == PARTIAL_FILL || this == TRADE;
    }

    /**
     * Check if this execution type represents a cancellation
     */
    public boolean isCancellation() {
        return this == CANCELED || this == TRADE_CANCEL;
    }

    /**
     * Check if this execution type affects position
     */
    public boolean affectsPosition() {
        return isTrade();
    }

    /**
     * Check if this execution type is final (no more updates expected)
     */
    public boolean isFinalStatus() {
        return this == FILL || this == CANCELED || this == REJECTED || this == EXPIRED;
    }

    @Override
    public String toString() {
        return code;
    }
}