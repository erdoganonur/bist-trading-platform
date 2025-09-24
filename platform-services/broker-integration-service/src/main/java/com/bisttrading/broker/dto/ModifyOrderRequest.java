package com.bisttrading.broker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * ModifyOrder API request
 * Python ModifyOrder() metodunun parametreleri
 */
@Data
public class ModifyOrderRequest {

    @JsonProperty("id")
    private String id; // Emrin ID'si

    @JsonProperty("price")
    private String price; // Düzeltilecek fiyat

    @JsonProperty("lot")
    private String lot; // Lot miktarı (VIOP emri ise girilmeli)

    @JsonProperty("viop")
    private Boolean viop; // VIOP emri mi

    @JsonProperty("subAccount")
    private String subAccount; // Alt hesap numarası
}