package com.example.paymentservice.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain aggregate: Payment. Created when we consume OrderCreated; updated when charge completes or refund is applied.
 */
public class Payment {

    private UUID paymentId;
    private UUID orderId;
    private String customerId;
    private PaymentStatus status;
    private BigDecimal amount;
    private String currency;
    private Instant createdAt;

    public Payment(UUID paymentId, UUID orderId, String customerId, BigDecimal amount, String currency, Instant createdAt) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.status = PaymentStatus.PENDING;
        this.amount = amount;
        this.currency = currency;
        this.createdAt = createdAt;
    }

    public void confirm() {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Payment can only be confirmed from PENDING; current: " + status);
        }
        this.status = PaymentStatus.CONFIRMED;
    }

    public void fail() {
        if (this.status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Payment can only be failed from PENDING; current: " + status);
        }
        this.status = PaymentStatus.FAILED;
    }

    public void refund() {
        if (this.status != PaymentStatus.CONFIRMED) {
            throw new IllegalStateException("Only CONFIRMED payments can be refunded; current: " + status);
        }
        this.status = PaymentStatus.REFUNDED;
    }

    public UUID getPaymentId() { return paymentId; }
    public UUID getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public PaymentStatus getStatus() { return status; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public Instant getCreatedAt() { return createdAt; }
}
