package com.example.orderservice.infrastructure.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.kafka.topics")
public class KafkaTopicsConfig {

    private String orderCreated = "order.created";
    private String orderConfirmed = "order.confirmed";
    private String inventoryReserved = "inventory.reserved";
    private String paymentFailed = "payment.failed";
    private String inventoryFailed = "inventory.failed";

    public String getOrderCreated() { return orderCreated; }
    public void setOrderCreated(String orderCreated) { this.orderCreated = orderCreated; }
    public String getOrderConfirmed() { return orderConfirmed; }
    public void setOrderConfirmed(String orderConfirmed) { this.orderConfirmed = orderConfirmed; }
    public String getInventoryReserved() { return inventoryReserved; }
    public void setInventoryReserved(String inventoryReserved) { this.inventoryReserved = inventoryReserved; }
    public String getPaymentFailed() { return paymentFailed; }
    public void setPaymentFailed(String paymentFailed) { this.paymentFailed = paymentFailed; }
    public String getInventoryFailed() { return inventoryFailed; }
    public void setInventoryFailed(String inventoryFailed) { this.inventoryFailed = inventoryFailed; }
}
