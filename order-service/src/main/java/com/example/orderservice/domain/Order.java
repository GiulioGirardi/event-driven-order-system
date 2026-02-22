package com.example.orderservice.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Domain aggregate: Order. Mutable only through application use cases;
 * infrastructure maps this to/from persistence.
 */
public class Order {

    private UUID orderId;
    private String customerId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String currency;
    private List<OrderItem> items;
    private Instant createdAt;

    public Order(UUID orderId, String customerId, BigDecimal totalAmount, String currency,
                 List<OrderItem> items, Instant createdAt) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.status = OrderStatus.PENDING;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.items = items;
        this.createdAt = createdAt;
    }

    public void confirm() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("Order can only be confirmed from PENDING; current: " + status);
        }
        this.status = OrderStatus.CONFIRMED;
    }

    public void fail() {
        this.status = OrderStatus.FAILED;
    }

    public UUID getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getCurrency() { return currency; }
    public List<OrderItem> getItems() { return items; }
    public Instant getCreatedAt() { return createdAt; }

    public record OrderItem(String productId, int quantity, BigDecimal unitPrice) {}
}
