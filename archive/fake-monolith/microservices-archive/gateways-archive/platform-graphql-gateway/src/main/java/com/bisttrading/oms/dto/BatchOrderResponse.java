package com.bisttrading.oms.dto;

import java.util.List;

public class BatchOrderResponse {
    private List<OrderResponse> orders;
    private List<String> errors;

    public List<OrderResponse> getOrders() { return orders; }
    public void setOrders(List<OrderResponse> orders) { this.orders = orders; }
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
}