package com.example.paymentservice.infrastructure.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.kafka.topics")
public class KafkaTopicsConfig {

    private String orderCreated = "order.created";
    private String paymentConfirmed = "payment.confirmed";
    private String paymentFailed = "payment.failed";
    private String paymentRefundRequested = "payment.refund.requested";

    public String getOrderCreated() { return orderCreated; }
    public void setOrderCreated(String orderCreated) { this.orderCreated = orderCreated; }
    public String getPaymentConfirmed() { return paymentConfirmed; }
    public void setPaymentConfirmed(String paymentConfirmed) { this.paymentConfirmed = paymentConfirmed; }
    public String getPaymentFailed() { return paymentFailed; }
    public void setPaymentFailed(String paymentFailed) { this.paymentFailed = paymentFailed; }
    public String getPaymentRefundRequested() { return paymentRefundRequested; }
    public void setPaymentRefundRequested(String paymentRefundRequested) { this.paymentRefundRequested = paymentRefundRequested; }
}
