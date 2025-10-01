package com.bisttrading.oms.dto;

import java.math.BigDecimal;

public class UpdateOrderRequest {
    private BigDecimal quantity;
    private BigDecimal price;
    private String timeInForce;

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getTimeInForce() { return timeInForce; }
    public void setTimeInForce(String timeInForce) { this.timeInForce = timeInForce; }
}