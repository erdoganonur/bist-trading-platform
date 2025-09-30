package com.bisttrading.broker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DeleteOrder API request
 * Python DeleteOrder() metodunun parametreleri
 */
@Data
public class DeleteOrderRequest {

    @JsonProperty("id")
    private String id; // Emrin ID'si

    @JsonProperty("subAccount")
    private String subAccount; // Alt hesap numarası

    @JsonProperty("adet") // VIOP emri için
    private String amount; // İptal edilecek adet (VIOP için)
}