package com.bisttrading.broker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * AlgoLab API'den gelen genel response formatı
 * Python'da resp.json() ile parse edilen yapı
 */
@Data
public class AlgoLabResponse<T> {

    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("message")
    private String message;

    @JsonProperty("content")
    private T content;

    /**
     * Başarılı response olup olmadığını kontrol eder
     */
    public boolean isSuccessful() {
        return Boolean.TRUE.equals(success);
    }

    /**
     * Hata mesajını döner (Türkçe)
     */
    public String getErrorMessage() {
        if (isSuccessful()) {
            return null;
        }
        return message != null ? message : "Bilinmeyen hata oluştu";
    }
}