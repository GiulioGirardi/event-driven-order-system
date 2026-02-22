package com.example.paymentservice.application.port;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Port for the external payment provider. Infrastructure can stub (e.g. fail for certain customerId/amount)
 * or integrate with a real gateway.
 */
public interface ChargeGateway {

    ChargeResult charge(UUID orderId, BigDecimal amount, String currency, String customerId);

    record ChargeResult(boolean success, String failureReason) {}
}
