package com.example.orderservice.infrastructure.persistence;

import com.example.orderservice.domain.OrderStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
class OrderEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID orderId;

    private String customerId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(precision = 19, scale = 4)
    private BigDecimal totalAmount;

    private String currency;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<OrderItemEmbeddable> items;

    private Instant createdAt;

    @Version
    private long version;

    protected OrderEntity() {}

    OrderEntity(UUID orderId, String customerId, OrderStatus status, BigDecimal totalAmount,
                String currency, List<OrderItemEmbeddable> items, Instant createdAt) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.items = items;
        this.createdAt = createdAt;
    }

    @Embeddable
    public static class OrderItemEmbeddable {
        public String productId;
        public int quantity;
        public BigDecimal unitPrice;

        public OrderItemEmbeddable() {}

        public OrderItemEmbeddable(String productId, int quantity, BigDecimal unitPrice) {
            this.productId = productId;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }
    }

    UUID getOrderId() { return orderId; }
    String getCustomerId() { return customerId; }
    OrderStatus getStatus() { return status; }
    void setStatus(OrderStatus status) { this.status = status; }
    BigDecimal getTotalAmount() { return totalAmount; }
    String getCurrency() { return currency; }
    List<OrderItemEmbeddable> getItems() { return items; }
    Instant getCreatedAt() { return createdAt; }
}
