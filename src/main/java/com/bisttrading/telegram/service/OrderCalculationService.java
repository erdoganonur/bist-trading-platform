package com.bisttrading.telegram.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Service for calculating order costs, commissions, and fees.
 * Helps users understand total cost before placing an order.
 */
@Slf4j
@Service
public class OrderCalculationService {

    // Borsa İstanbul commission rates
    private static final BigDecimal COMMISSION_RATE = new BigDecimal("0.002"); // %0.2
    private static final BigDecimal BSMV_RATE = new BigDecimal("0.001"); // %0.1 on commission
    private static final BigDecimal BIST_FEE_RATE = new BigDecimal("0.00003"); // %0.003

    /**
     * Calculate order estimate including all fees and commissions.
     *
     * @param quantity Order quantity (adet)
     * @param price Price per share
     * @param isBuy true for buy order, false for sell order
     * @return Order estimate with breakdown
     */
    public OrderEstimate calculateOrderCost(int quantity, BigDecimal price, boolean isBuy) {
        log.debug("Calculating order cost: qty={}, price={}, isBuy={}", quantity, price, isBuy);

        // Base value = quantity × price
        BigDecimal baseValue = price.multiply(BigDecimal.valueOf(quantity));

        // Commission = baseValue × commission rate
        BigDecimal commission = baseValue.multiply(COMMISSION_RATE).setScale(2, RoundingMode.HALF_UP);

        // BSMV = commission × BSMV rate
        BigDecimal bsmv = commission.multiply(BSMV_RATE).setScale(2, RoundingMode.HALF_UP);

        // BIST fee = baseValue × BIST fee rate
        BigDecimal bistFee = baseValue.multiply(BIST_FEE_RATE).setScale(2, RoundingMode.HALF_UP);

        // Total fees
        BigDecimal totalFees = commission.add(bsmv).add(bistFee);

        // Grand total
        BigDecimal grandTotal;
        if (isBuy) {
            // For buy: total = baseValue + fees
            grandTotal = baseValue.add(totalFees);
        } else {
            // For sell: total = baseValue - fees
            grandTotal = baseValue.subtract(totalFees);
        }

        return OrderEstimate.builder()
                .baseValue(baseValue)
                .commission(commission)
                .bsmv(bsmv)
                .bistFee(bistFee)
                .totalFees(totalFees)
                .grandTotal(grandTotal)
                .isBuy(isBuy)
                .build();
    }

    /**
     * Format order estimate as a readable message.
     *
     * @param estimate Order estimate
     * @return Formatted message
     */
    public String formatEstimate(OrderEstimate estimate) {
        StringBuilder sb = new StringBuilder();
        sb.append("💰 *Maliyet Özeti*\n\n");
        sb.append(String.format("Toplam Değer: ₺%.2f\n", estimate.getBaseValue()));
        sb.append(String.format("Komisyon: ₺%.2f\n", estimate.getCommission()));
        sb.append(String.format("BSMV: ₺%.2f\n", estimate.getBsmv()));
        sb.append(String.format("Borsa Ücreti: ₺%.2f\n", estimate.getBistFee()));
        sb.append("━━━━━━━━━━━━━━━\n");

        if (estimate.isBuy()) {
            sb.append(String.format("*Ödenecek Tutar: ₺%.2f*\n", estimate.getGrandTotal()));
        } else {
            sb.append(String.format("*Alınacak Tutar: ₺%.2f*\n", estimate.getGrandTotal()));
        }

        return sb.toString();
    }

    /**
     * Order cost estimate.
     */
    @Data
    @lombok.Builder
    public static class OrderEstimate {
        private BigDecimal baseValue;      // quantity × price
        private BigDecimal commission;     // Commission fee
        private BigDecimal bsmv;          // BSMV tax
        private BigDecimal bistFee;       // BIST exchange fee
        private BigDecimal totalFees;     // Sum of all fees
        private BigDecimal grandTotal;    // Final total (baseValue ± fees)
        private boolean isBuy;            // Buy or sell order
    }
}
