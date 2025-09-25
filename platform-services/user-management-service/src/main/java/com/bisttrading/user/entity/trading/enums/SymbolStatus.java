package com.bisttrading.user.entity.trading.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Enumeration for symbol trading status
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since Sprint 3
 */
@Getter
public enum SymbolStatus {
    ACTIVE("ACTIVE", "Actively Trading", "Aktif İşlem Görüyor"),
    SUSPENDED("SUSPENDED", "Trading Suspended", "İşlem Askıya Alındı"),
    HALTED("HALTED", "Trading Halted", "İşlem Durduruldu"),
    DELISTED("DELISTED", "Delisted", "Kotasyondan Çıkarıldı"),
    PRE_TRADING("PRE_TRADING", "Pre-Market Trading", "Açılış Öncesi İşlem"),
    POST_TRADING("POST_TRADING", "After-Hours Trading", "Kapanış Sonrası İşlem");

    private final String code;
    private final String description;
    private final String turkishDescription;

    SymbolStatus(String code, String description, String turkishDescription) {
        this.code = code;
        this.description = description;
        this.turkishDescription = turkishDescription;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public static SymbolStatus fromCode(String code) {
        for (SymbolStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown symbol status code: " + code);
    }

    public boolean isTradeable() {
        return this == ACTIVE || this == PRE_TRADING || this == POST_TRADING;
    }

    @Override
    public String toString() {
        return code;
    }
}