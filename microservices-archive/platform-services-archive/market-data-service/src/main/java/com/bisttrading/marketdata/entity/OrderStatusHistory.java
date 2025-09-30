package com.bisttrading.marketdata.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order status geçmişini TimescaleDB'de saklayan entity
 * Hypertable: order_status_history (time partitioned)
 */
@Entity
@Table(name = "order_status_history", indexes = {
    @Index(name = "idx_order_status_order_id_time", columnList = "orderId, timestamp"),
    @Index(name = "idx_order_status_time", columnList = "timestamp"),
    @Index(name = "idx_order_status_order_id", columnList = "orderId"),
    @Index(name = "idx_order_status_symbol", columnList = "symbol")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, length = 50)
    private String orderId;

    @Column(length = 10)
    private String symbol;

    @Column(nullable = false)
    private Integer status;

    @Column(name = "status_text", length = 50)
    private String statusText;

    @Column
    private Long quantity;

    @Column(name = "filled_quantity")
    private Long filledQuantity;

    @Column(precision = 18, scale = 8)
    private BigDecimal price;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    /**
     * Builder pattern için convenience constructor
     */
    public static OrderStatusHistory builder() {
        return new OrderStatusHistory();
    }

    public OrderStatusHistory orderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public OrderStatusHistory symbol(String symbol) {
        this.symbol = symbol;
        return this;
    }

    public OrderStatusHistory status(Integer status) {
        this.status = status;
        return this;
    }

    public OrderStatusHistory statusText(String statusText) {
        this.statusText = statusText;
        return this;
    }

    public OrderStatusHistory quantity(Long quantity) {
        this.quantity = quantity;
        return this;
    }

    public OrderStatusHistory filledQuantity(Long filledQuantity) {
        this.filledQuantity = filledQuantity;
        return this;
    }

    public OrderStatusHistory price(BigDecimal price) {
        this.price = price;
        return this;
    }

    public OrderStatusHistory timestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }
}