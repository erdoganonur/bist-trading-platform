package com.bisttrading.broker.dto;

import lombok.Getter;

/**
 * AlgoLab emir durumları
 * Python karşılığı: config.py dosyasındaki ORDER_STATUS dictionary
 */
@Getter
public enum OrderStatus {
    WAITING(0, "Bekleyen"),
    DELIVERED(1, "Teslim Edildi"),
    EXECUTED(2, "Gerçekleşti"),
    PARTIALLY_EXECUTED(3, "Kısmi Gerçekleşti"),
    CANCELLED(4, "İptal Edildi"),
    MODIFIED(5, "Değiştirildi"),
    SUSPENDED(6, "Askıya Alındı"),
    EXPIRED(7, "Süresi Doldu"),
    ERROR(8, "Hata");

    private final int code;
    private final String description;

    OrderStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Code'dan OrderStatus döner
     */
    public static OrderStatus fromCode(int code) {
        for (OrderStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Geçersiz emir durum kodu: " + code);
    }

    /**
     * String açıklamadan OrderStatus döner
     */
    public static OrderStatus fromDescription(String description) {
        for (OrderStatus status : values()) {
            if (status.description.equalsIgnoreCase(description)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Geçersiz emir durum açıklaması: " + description);
    }
}