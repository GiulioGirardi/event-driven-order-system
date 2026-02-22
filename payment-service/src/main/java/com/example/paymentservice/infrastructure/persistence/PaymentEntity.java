package com.example.paymentservice.infrastructure.persistence;

import com.example.paymentservice.domain.PaymentStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments", indexes = @Index(columnList = "order_id", unique = true))
class PaymentEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID paymentId;

    @Column(name = "order_id", nullable = false, columnDefinition = "uuid")
    private UUID orderId;

    private String customerId;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(precision = 19, scale = 4)
    private BigDecimal amount;

    private String currency;

    private Instant createdAt;

    @Version
    private long version;

    protected PaymentEntity() {}

    PaymentEntity(UUID paymentId, UUID orderId, String customerId, PaymentStatus status,
                  BigDecimal amount, String currency, Instant createdAt) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.status = status;
        this.amount = amount;
        this.currency = currency;
        this.createdAt = createdAt;
    }

    UUID getPaymentId() { return paymentId; }
    UUID getOrderId() { return orderId; }
    String getCustomerId() { return customerId; }
    PaymentStatus getStatus() { return status; }
    void setStatus(PaymentStatus status) { this.status = status; }
    BigDecimal getAmount() { return amount; }
    String getCurrency() { return currency; }
    Instant getCreatedAt() { return createdAt; }
}
