package com.bisttrading.broker.algolab.exception;

public class AlgoLabOrderException extends AlgoLabException {

    private final String orderId;
    private final String clientOrderId;

    public AlgoLabOrderException(String message) {
        super(message, "ORDER_ERROR", 422);
        this.orderId = null;
        this.clientOrderId = null;
    }

    public AlgoLabOrderException(String message, String orderId) {
        super(message, "ORDER_ERROR", 422);
        this.orderId = orderId;
        this.clientOrderId = null;
    }

    public AlgoLabOrderException(String message, String orderId, String clientOrderId) {
        super(message, "ORDER_ERROR", 422);
        this.orderId = orderId;
        this.clientOrderId = clientOrderId;
    }

    public AlgoLabOrderException(String message, Throwable cause) {
        super(message, "ORDER_ERROR", 422, cause);
        this.orderId = null;
        this.clientOrderId = null;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getClientOrderId() {
        return clientOrderId;
    }

    public boolean hasOrderId() {
        return orderId != null && !orderId.trim().isEmpty();
    }

    public boolean hasClientOrderId() {
        return clientOrderId != null && !clientOrderId.trim().isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());

        if (hasOrderId()) {
            sb.append(" [OrderId: ").append(orderId).append("]");
        }

        if (hasClientOrderId()) {
            sb.append(" [ClientOrderId: ").append(clientOrderId).append("]");
        }

        return sb.toString();
    }
}