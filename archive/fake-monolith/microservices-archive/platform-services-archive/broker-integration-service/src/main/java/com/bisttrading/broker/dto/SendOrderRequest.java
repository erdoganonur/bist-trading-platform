package com.bisttrading.broker.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * SendOrder API request
 * Python SendOrder() metodunun parametreleri
 */
@Data
public class SendOrderRequest {

    @JsonProperty("symbol")
    private String symbol; // Sembol kodu (örn: TSKB)

    @JsonProperty("direction")
    private String direction; // BUY / SELL

    @JsonProperty("pricetype")
    private String priceType; // piyasa / limit

    @JsonProperty("price")
    private String price; // Emir fiyatı (limit emrinde)

    @JsonProperty("lot")
    private String lot; // Emir adeti

    @JsonProperty("sms")
    private Boolean sms; // SMS gönderim

    @JsonProperty("email")
    private Boolean email; // Email gönderim

    @JsonProperty("subAccount")
    private String subAccount; // Alt hesap numarası
}