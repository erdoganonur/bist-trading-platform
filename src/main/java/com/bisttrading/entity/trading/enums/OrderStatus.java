package com.bisttrading.entity.trading.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Enumeration for order status throughout its lifecycle
 *
 * @author BIST Trading Platform
 * @version 1.0
 * @since Sprint 3
 */
@Getter
public enum OrderStatus {
    PENDING("PENDING", "Pending", "Beklemede", "Order created but not yet submitted"),
    SUBMITTED("SUBMITTED", "Submitted", "Gönderildi", "Order submitted to broker"),
    ACCEPTED("ACCEPTED", "Accepted", "Kabul Edildi", "Order accepted by exchange"),
    REJECTED("REJECTED", "Rejected", "Reddedildi", "Order rejected by broker or exchange"),
    PARTIALLY_FILLED("PARTIALLY_FILLED", "Partially Filled", "Kısmen Gerçekleşti", "Order partially executed"),
    FILLED("FILLED", "Filled", "Gerçekleşti", "Order completely filled"),
    CANCELLED("CANCELLED", "Cancelled", "İptal Edildi", "Order cancelled by user or system"),
    REPLACED("REPLACED", "Replaced", "Değiştirildi", "Order was replaced (modified)"),
    SUSPENDED("SUSPENDED", "Suspended", "Askıya Alındı", "Order suspended (e.g., market halt)"),
    EXPIRED("EXPIRED", "Expired", "Süresi Doldu", "Order expired (GTD orders)"),
    ERROR("ERROR", "Error", "Hata", "System error occurred");

    private final String code;
    private final String description;
    private final String turkishDescription;
    private final String explanation;

    OrderStatus(String code, String description, String turkishDescription, String explanation) {
        this.code = code;
        this.description = description;
        this.turkishDescription = turkishDescription;
        this.explanation = explanation;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public static OrderStatus fromCode(String code) {
        for (OrderStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown order status code: " + code);
    }

    /**
     * Check if the order is still active (can be filled)
     */
    public boolean isActive() {
        return this == SUBMITTED || this == ACCEPTED || this == PARTIALLY_FILLED;
    }

    /**
     * Check if the order is completed (no more fills expected)
     */
    public boolean isCompleted() {
        return this == FILLED || this == CANCELLED || this == REJECTED || this == EXPIRED || this == ERROR;
    }

    /**
     * Check if the order can be cancelled
     */
    public boolean isCancellable() {
        return isActive() && this != SUSPENDED;
    }

    /**
     * Check if the order can be modified
     */
    public boolean isModifiable() {
        return this == SUBMITTED || this == ACCEPTED;
    }

    /**
     * Check if the order has execution activity
     */
    public boolean hasExecutions() {
        return this == PARTIALLY_FILLED || this == FILLED;
    }

    /**
     * Get valid transition states from current status
     */
    public OrderStatus[] getValidTransitions() {
        switch (this) {
            case PENDING:
                return new OrderStatus[]{SUBMITTED, REJECTED, ERROR};
            case SUBMITTED:
                return new OrderStatus[]{ACCEPTED, REJECTED, CANCELLED, SUSPENDED, ERROR};
            case ACCEPTED:
                return new OrderStatus[]{PARTIALLY_FILLED, FILLED, CANCELLED, SUSPENDED, REPLACED, EXPIRED, ERROR};
            case PARTIALLY_FILLED:
                return new OrderStatus[]{FILLED, CANCELLED, SUSPENDED, REPLACED, ERROR};
            case SUSPENDED:
                return new OrderStatus[]{ACCEPTED, CANCELLED, ERROR};
            default:
                return new OrderStatus[]{};
        }
    }

    /**
     * Check if transition to new status is valid
     */
    public boolean canTransitionTo(OrderStatus newStatus) {
        OrderStatus[] validTransitions = getValidTransitions();
        for (OrderStatus validStatus : validTransitions) {
            if (validStatus == newStatus) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return code;
    }
}