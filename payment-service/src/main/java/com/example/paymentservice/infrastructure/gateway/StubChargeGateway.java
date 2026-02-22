package com.example.paymentservice.infrastructure.gateway;

import com.example.paymentservice.application.port.ChargeGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Stub payment provider: fails for customerId "fail-payment" or amount > failAboveAmount (for testing).
 * Otherwise always succeeds.
 */
@Component
class StubChargeGateway implements ChargeGateway {

    private static final Logger log = LoggerFactory.getLogger(StubChargeGateway.class);

    @Value("${app.payment.stub.fail-customer-id:fail-payment}")
    private String failCustomerId;

    @Value("${app.payment.stub.fail-above-amount:0}")
    private BigDecimal failAboveAmount;

    @Override
    public ChargeResult charge(UUID orderId, BigDecimal amount, String currency, String customerId) {
        if (failCustomerId != null && failCustomerId.equals(customerId)) {
            log.info("Stub: rejecting charge for customerId={}", customerId);
            return new ChargeResult(false, "Stub: customer marked for failure");
        }
        if (failAboveAmount != null && failAboveAmount.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(failAboveAmount) > 0) {
            log.info("Stub: rejecting charge amount {} > {}", amount, failAboveAmount);
            return new ChargeResult(false, "Stub: amount above threshold");
        }
        return new ChargeResult(true, null);
    }
}
