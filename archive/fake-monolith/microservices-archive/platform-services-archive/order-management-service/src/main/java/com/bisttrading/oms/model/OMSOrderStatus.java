package com.bisttrading.oms.model;

/**
 * OMS specific order status enum
 */
public enum OMSOrderStatus {
    PENDING,             // Order is being processed by OMS
    SUBMITTED,           // Order submitted to broker
    ACTIVE,              // Order is active in the market
    FILLED,              // Order fully filled
    PARTIAL,             // Order partially filled
    PARTIALLY_FILLED,    // Order partially filled (alias)
    CANCELLED,           // Order cancelled
    REJECTED,            // Order rejected
    EXPIRED,             // Order expired
    FAILED,              // Technical failure
    ACCEPTED,            // Order accepted
    CANCEL_REJECTED,     // Order cancel rejected
    MODIFY_REJECTED,     // Order modification rejected
    PENDING_CANCEL,      // Order pending cancellation
    SUSPENDED            // Order suspended
}