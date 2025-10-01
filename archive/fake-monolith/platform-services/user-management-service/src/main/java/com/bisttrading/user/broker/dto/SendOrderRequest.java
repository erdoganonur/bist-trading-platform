package com.bisttrading.user.broker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for placing a new order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendOrderRequest {

    @NotBlank(message = "Hisse senedi sembolü gereklidir")
    private String symbol;

    @NotBlank(message = "Emir yönü gereklidir (BUY/SELL)")
    private String direction;

    @NotBlank(message = "Fiyat tipi gereklidir (LIMIT/MARKET)")
    private String priceType;

    @NotNull(message = "Fiyat gereklidir")
    @Positive(message = "Fiyat pozitif olmalıdır")
    private BigDecimal price;

    @NotNull(message = "Lot miktarı gereklidir")
    @Positive(message = "Lot miktarı pozitif olmalıdır")
    private Integer lot;

    private Boolean sms = false;
    private Boolean email = false;
    private String subAccount;
}