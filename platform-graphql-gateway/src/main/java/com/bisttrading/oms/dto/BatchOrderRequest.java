package com.bisttrading.oms.dto;

import java.util.List;

public class BatchOrderRequest {
    private List<CreateOrderRequest> orders;

    public List<CreateOrderRequest> getOrders() { return orders; }
    public void setOrders(List<CreateOrderRequest> orders) { this.orders = orders; }
}