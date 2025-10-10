package com.bisttrading.broker.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for modifying an existing order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModifyOrderRequest {

    @NotNull(message = "Fiyat gereklidir")
    @Positive(message = "Fiyat pozitif olmalıdır")
    private BigDecimal price;

    @NotNull(message = "Lot miktarı gereklidir")
    @Positive(message = "Lot miktarı pozitif olmalıdır")
    private Integer lot;

    private String viop;
    private String subAccount;
}