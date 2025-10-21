package com.bisttrading.broker.dto;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for modifying an existing order.
 * At least one of price or lot must be provided.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModifyOrderRequest {

    // Optional - only required if modifying price
    @Positive(message = "Fiyat pozitif olmalıdır")
    private BigDecimal price;

    // Optional - only required if modifying quantity
    @Positive(message = "Lot miktarı pozitif olmalıdır")
    private Integer lot;

    private String viop;
    private String subAccount;
}